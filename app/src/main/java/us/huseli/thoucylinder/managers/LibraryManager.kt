package us.huseli.thoucylinder.managers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.MAX_CONCURRENT_TRACK_DOWNLOADS
import us.huseli.thoucylinder.DownloadTaskState
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.LocalImportableAlbum
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.abstr.toArtists
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.listCoverImages
import us.huseli.thoucylinder.dataclasses.musicBrainz.capitalizeGenreName
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.getBitmap
import us.huseli.thoucylinder.getSquareSize
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryManager @Inject constructor(
    private val repos: Repositories,
    private val database: Database,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), ILogger {
    private val _albumDownloadTasks = MutableStateFlow<List<AlbumDownloadTask>>(emptyList())
    private val _trackDownloadTasks = MutableStateFlow<ImmutableList<TrackDownloadTask>>(persistentListOf())
    private val _runningTasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())

    val albumDownloadTasks = _albumDownloadTasks.asStateFlow()
    val trackDownloadTasks = _trackDownloadTasks.asStateFlow()

    init {
        doStartupTasks()

        launchOnIOThread {
            _runningTasks.collect { runningTasks ->
                if (runningTasks.size < MAX_CONCURRENT_TRACK_DOWNLOADS) {
                    _trackDownloadTasks.value.find { it.state.value == DownloadTaskState.CREATED }?.start()
                }
            }
        }

        repos.musicBrainz.startMatchingArtists(repos.artist.artistsWithTracksOrAlbums) { artistId, musicBrainzId ->
            repos.artist.setArtistMusicBrainzId(artistId, musicBrainzId)
        }

        repos.spotify.startMatchingArtists(repos.artist.artistsWithTracksOrAlbums) { artistId, spotifyId, image ->
            repos.artist.setArtistSpotifyData(artistId, spotifyId, image)
        }
    }


    /** PUBLIC METHODS ************************************************************************************************/

    fun cancelAlbumDownload(albumId: String) {
        _albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    suspend fun deleteLocalAlbumFiles(albumIds: Collection<String>, updateDb: Boolean = true) {
        val combos = repos.album.listAlbumsWithTracks(albumIds)

        if (combos.isNotEmpty()) deleteLocalAlbumComboFiles(combos, updateDb)
    }

    fun doStartupTasks() {
        launchOnIOThread {
            updateGenreList()
            if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums2()
            handleOrphansAndDuplicates()
            repos.playlist.deleteOrphanPlaylistTracks()
            repos.track.deleteTempTracks()
            repos.album.deleteTempAlbums()
            deleteMarkedAlbums()
        }
    }

    fun downloadAlbum(
        albumId: String,
        onFinish: (hasErrors: Boolean) -> Unit,
        onTrackError: (TrackCombo, Throwable) -> Unit,
    ) {
        launchOnIOThread {
            repos.album.getAlbumWithTracks(albumId)?.also { albumCombo ->
                repos.settings.createAlbumDirectory(albumCombo)?.also { directory ->
                    val trackCombos = albumCombo.trackCombos.filter { !it.track.isDownloaded }
                    val trackTasks = trackCombos.map { trackCombo ->
                        createTrackDownloadTask(
                            combo = trackCombo,
                            directory = directory,
                            onError = { onTrackError(trackCombo, it) },
                        )
                    }
                    val albumArt = albumCombo.album.albumArt?.saveInternal(albumCombo.album, context)
                    val album = albumCombo.album.copy(
                        isLocal = true,
                        isInLibrary = true,
                        albumArt = albumArt,
                    )

                    albumArt?.saveToDirectory(context, directory)
                    _albumDownloadTasks.value += AlbumDownloadTask(
                        album = album,
                        trackTasks = trackTasks,
                        onFinish = onFinish,
                    )
                    repos.album.updateAlbum(album)
                }
            }
        }
    }

    fun downloadTrack(trackId: String) {
        launchOnIOThread {
            repos.settings.getLocalMusicDirectory()?.also { directory ->
                repos.track.getTrackComboById(trackId)?.also { combo ->
                    createTrackDownloadTask(combo = combo, directory = directory)
                }
            }
        }
    }

    suspend fun ensureTrackMetadata(trackId: String, forceReload: Boolean = false, commit: Boolean = true): Track? =
        repos.track.getTrackById(trackId)?.let {
            repos.youtube.ensureTrackMetadata(track = it, forceReload = forceReload) { track ->
                if (commit) repos.track.updateTrack(track)
            }
        }

    suspend fun ensureTrackMetadata(track: Track, forceReload: Boolean = false, commit: Boolean = true): Track =
        repos.youtube.ensureTrackMetadata(track = track, forceReload = forceReload) {
            if (commit) repos.track.updateTrack(it)
        }

    fun ensureTrackMetadataAsync(trackId: String) {
        launchOnIOThread {
            repos.track.getTrackById(trackId)?.also { ensureTrackMetadata(track = it) }
        }
    }

    fun ensureTrackMetadataAsync(track: Track) {
        launchOnIOThread { ensureTrackMetadata(track = track) }
    }

    fun importNewLocalAlbumsAsync() {
        launchOnIOThread { importNewLocalAlbums2() }
    }

    suspend fun upsertAlbumCombo(combo: AlbumWithTracksCombo) {
        database.withTransaction {
            repos.album.upsertAlbumAndTags(combo)
            repos.track.setAlbumTracks(combo.album.albumId, combo.tracks)
            repos.artist.setAlbumArtists(combo.album.albumId, combo.artists.toAlbumArtists())
            repos.artist.clearTrackArtists(combo.trackIds)
            repos.artist.insertTrackArtists(combo.trackCombos.flatMap { it.artists.toTrackArtists() })

            combo.artists
                .plus(combo.trackCombos.flatMap { it.artists })
                .toArtists()
                .toSet()
                .forEach { artist ->
                    artist.spotifyId?.also { repos.artist.setArtistSpotifyId(artist.artistId, it) }
                    artist.musicBrainzId?.also { repos.artist.setArtistMusicBrainzId(artist.artistId, it) }
                }
        }
    }

    fun updateAlbumFromMusicBrainz(albumId: String) {
        launchOnIOThread { repos.album.getAlbumWithTracks(albumId)?.also { updateAlbumFromMusicBrainz(it) } }
    }

    fun updateAlbumFromMusicBrainzAsync(
        combo: AlbumWithTracksCombo,
        strategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
    ) {
        launchOnIOThread { updateAlbumFromMusicBrainz(combo, strategy) }
    }

    suspend fun updateTrack(
        uiState: TrackUiState,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumPosition: Int? = null,
        discNumber: Int? = null,
    ) {
        val track = repos.track.getTrackById(uiState.trackId)

        if (track != null) updateTrack(
            track = track,
            trackArtists = uiState.trackArtists,
            title = title,
            year = year,
            artistNames = artistNames,
            albumPosition = albumPosition,
            discNumber = discNumber,
        )
    }

    suspend fun updateTrack(
        track: Track,
        trackArtists: Collection<TrackArtistCredit>,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumPosition: Int? = null,
        discNumber: Int? = null,
        albumCombo: AbstractAlbumCombo? = null,
    ) {
        var finalTrackArtists = trackArtists.toList()
        val finalAlbumCombo = albumCombo ?: track.albumId?.let { repos.album.getAlbumCombo(it) }

        if (artistNames.filter { it.isNotEmpty() } != trackArtists.map { it.name }) {
            finalTrackArtists = artistNames.filter { it.isNotEmpty() }
                .map { repos.artist.artistCache.getByName(it) }
                .map { TrackArtistCredit(artist = it, trackId = track.trackId) }
            repos.artist.setTrackArtists(track.trackId, finalTrackArtists.toTrackArtists())
        }

        tagAndUpdateTrack(
            track = track.copy(
                title = title,
                year = year,
                albumPosition = albumPosition ?: track.albumPosition,
                discNumber = discNumber ?: track.discNumber,
            ),
            trackArtists = finalTrackArtists,
            album = finalAlbumCombo?.album,
            albumArtists = finalAlbumCombo?.artists,
        )
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun createTrackDownloadTask(
        combo: AbstractTrackCombo,
        directory: DocumentFile,
        onError: (Throwable) -> Unit = {},
    ): TrackDownloadTask {
        val task = TrackDownloadTask(
            scope = scope,
            track = combo.track,
            trackArtists = combo.artists,
            directory = directory,
            repos = repos,
            album = combo.album,
            albumArtists = combo.albumArtists,
            onError = onError,
        )

        _trackDownloadTasks.value = _trackDownloadTasks.value.plus(task).toImmutableList()
        if (_runningTasks.value.size < MAX_CONCURRENT_TRACK_DOWNLOADS) task.start()

        scope.launch {
            task.state.collect { state ->
                if (state == DownloadTaskState.RUNNING) {
                    if (!_runningTasks.value.contains(task)) _runningTasks.value += task
                } else _runningTasks.value -= task
            }
        }

        return task
    }

    private suspend fun deleteLocalAlbumComboFiles(combos: Collection<AlbumWithTracksCombo>, updateDb: Boolean = true) {
        val tracks = combos.flatMap { it.tracks }

        combos.forEach { combo ->
            repos.localMedia.deleteLocalAlbumArt(
                albumCombo = combo,
                albumDirectory = repos.settings.getAlbumDirectory(combo),
            )
        }

        if (tracks.isNotEmpty()) repos.track.deleteTrackFiles(tracks)

        if (updateDb) database.withTransaction {
            repos.album.setAlbumsIsLocal(combos.map { it.album.albumId }, false)
            combos.filter { it.album.albumArt?.isLocal == true }.forEach { repos.album.clearAlbumArt(it.album.albumId) }
            if (tracks.isNotEmpty()) repos.track.clearLocalUris(tracks.map { it.trackId })
        }
    }

    private suspend fun deleteMarkedAlbums() {
        val combos = repos.album.listDeletionMarkedAlbumCombos()

        if (combos.isNotEmpty()) {
            deleteLocalAlbumComboFiles(combos, updateDb = false)
            database.withTransaction {
                repos.track.deleteTracksByAlbumId(combos.map { it.album.albumId })
                repos.album.deleteAlbums(combos.map { it.album })
            }
        }
    }

    private suspend fun handleOrphansAndDuplicates() {
        val allAlbumsWithTracks = repos.album.listAlbumsWithTracks()
        val albumTracks = allAlbumsWithTracks.flatMap { combo -> combo.tracks }
        val nonAlbumTracks = repos.track.listNonAlbumTracks()
        val allTracks = albumTracks + nonAlbumTracks
        // Collect tracks with non-working localUris:
        val brokenUriTrackIds = repos.localMedia.listTracksWithBrokenLocalUris(allTracks).map { it.trackId }
        val nonLocalTrackIds = brokenUriTrackIds + allTracks.filter { it.localUri == null }.map { it.trackId }
        // Collect albums that have isLocal=true but should have false:
        val noLongerLocalAlbumsIds = allAlbumsWithTracks
            .filter { combo -> combo.album.isLocal && nonLocalTrackIds.containsAll(combo.trackIds) }
            .map { it.album.albumId }
        val duplicateNonAlbumTracks = nonAlbumTracks.filter { track ->
            albumTracks.find { it.localUri == track.localUri && it.youtubeVideo?.id == track.youtubeVideo?.id } != null
        }

        database.withTransaction {
            // Delete non-album tracks that have duplicates on albums:
            if (duplicateNonAlbumTracks.isNotEmpty()) repos.track.deleteTracks(duplicateNonAlbumTracks)
            // Update tracks with broken localUris:
            if (brokenUriTrackIds.isNotEmpty()) repos.track.clearLocalUris(brokenUriTrackIds)
            // Update albums that should have isLocal=true, but don't:
            if (noLongerLocalAlbumsIds.isNotEmpty()) repos.album.setAlbumsIsLocal(noLongerLocalAlbumsIds, false)
        }
    }

    suspend fun flowImportableAlbums(treeUri: Uri): Flow<LocalImportableAlbum> {
        return DocumentFile.fromTreeUri(context, treeUri)?.let {
            val existingTrackUris = repos.track.listTrackLocalUris()

            repos.localMedia.flowImportableAlbums(it, existingTrackUris)
        } ?: emptyFlow()
    }

    private suspend fun getBestNewLocalAlbumArt(trackCombos: Collection<AbstractTrackCombo>): MediaStoreImage? =
        trackCombos.map { it.track }
            .listCoverImages(context)
            .mapNotNull { it.uri.toMediaStoreImage(context) }
            .maxByOrNull { albumArt -> albumArt.fullUri.getBitmap(context)?.getSquareSize() ?: 0 }

    private suspend fun importNewLocalAlbums2() {
        if (!repos.localMedia.isImportingLocalMedia.value) {
            val localMusicDirectory =
                repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

            if (localMusicDirectory != null) {
                repos.localMedia.setIsImporting(true)

                val existingAlbumCombos = repos.album.listAlbumCombos()
                val existingTrackUris = repos.track.listTrackLocalUris()

                repos.localMedia.flowImportableAlbums(localMusicDirectory, existingTrackUris)
                    .onCompletion { repos.localMedia.setIsImporting(false) }
                    .collect { localAlbum ->
                        val existingCombo = existingAlbumCombos.find {
                            (it.album.title == localAlbum.title && it.artists.joined() == localAlbum.artistName) ||
                                (it.album.musicBrainzReleaseId != null && it.album.musicBrainzReleaseId == localAlbum.musicBrainzReleaseId)
                        }
                        val combo = localAlbum.toAlbumWithTracks(
                            base = existingCombo,
                            getArtist = { repos.artist.artistCache.get(it) },
                        ).let { combo ->
                            val albumArt = getBestNewLocalAlbumArt(combo.trackCombos)

                            if (albumArt != null) combo.copy(album = combo.album.copy(albumArt = albumArt))
                            else combo
                        }

                        upsertAlbumCombo(combo)
                        launchOnIOThread { updateAlbumFromMusicBrainz(combo, TrackMergeStrategy.KEEP_SELF) }
                    }
            }
        }
    }

    private suspend fun tagAndUpdateTrack(
        track: Track,
        trackArtists: List<AbstractArtistCredit>,
        album: Album? = null,
        albumArtists: List<AbstractArtistCredit>? = null,
    ): Track {
        val updatedTrack = ensureTrackMetadata(track = track, commit = false)

        repos.track.updateTrack(updatedTrack)
        repos.localMedia.tagTrack(
            track = updatedTrack,
            trackArtists = trackArtists,
            albumArtists = albumArtists,
            album = album,
        )

        return updatedTrack
    }

    private suspend fun updateAlbumFromMusicBrainz(
        combo: AlbumWithTracksCombo,
        strategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
    ) {
        if (combo.album.musicBrainzReleaseId == null) {
            val match = repos.musicBrainz.matchAlbumWithTracks(
                combo = combo,
                strategy = strategy,
                getArtist = { repos.artist.artistCache.get(it) },
            )

            if (match != null) database.withTransaction {
                repos.album.updateAlbum(match.album)
                repos.album.setAlbumTags(match.album.albumId, match.tags)
                repos.artist.setAlbumArtists(match.album.albumId, match.artists.toAlbumArtists())
                repos.track.setAlbumTracks(match.album.albumId, match.trackCombos.map { it.track })
                repos.artist.clearTrackArtists(match.trackIds)
                repos.artist.insertTrackArtists(match.trackCombos.flatMap { it.artists.toTrackArtists() })

                match.album.musicBrainzReleaseId?.also { releaseId ->
                    repos.musicBrainz.getReleaseCoverArt(releaseId)?.also {
                        repos.album.updateAlbumArt(match.album.albumId, it)
                    }
                }
            }
        }
    }

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        try {
            val existingGenreNames = repos.album.listTags().map { it.name.lowercase() }.toSet()
            val mbGenreNames = repos.musicBrainz.listAllGenreNames()
            val newTags = mbGenreNames
                .minus(existingGenreNames)
                .map { Tag(name = capitalizeGenreName(it), isMusicBrainzGenre = true) }

            if (newTags.isNotEmpty()) repos.album.insertTags(newTags)
        } catch (e: Exception) {
            logError("updateGenreList: $e", e)
        }
    }
}
