package us.huseli.thoucylinder.managers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.MAX_CONCURRENT_TRACK_DOWNLOADS
import us.huseli.thoucylinder.DownloadTaskState
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.XSPFPlaylist
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.musicbrainz.capitalizeGenreName
import us.huseli.thoucylinder.dataclasses.tag.Tag
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.track.ITrackCombo
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.dataclasses.track.listCoverImages
import us.huseli.thoucylinder.enums.ListUpdateStrategy
import us.huseli.thoucylinder.getBitmap
import us.huseli.thoucylinder.getSquareSize
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.repositories.Repositories
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LibraryManager @Inject constructor(
    private val repos: Repositories,
    private val database: Database,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), ILogger {
    private val _albumDownloadTasks = MutableStateFlow<List<AlbumDownloadTask>>(emptyList())
    private val _trackDownloadTasks = MutableStateFlow<ImmutableList<TrackDownloadTask>>(persistentListOf())
    private val _runningTasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())

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

    suspend fun addAlbumsToLibrary(
        albumIds: List<String>,
        onGotoLibraryClick: (() -> Unit)? = null,
        onGotoAlbumClick: ((String) -> Unit)? = null,
    ) {
        repos.album.addAlbumsToLibrary(albumIds)
        repos.track.addToLibraryByAlbumId(albumIds)
        repos.message.onAddAlbumsToLibrary(albumIds, onGotoLibraryClick, onGotoAlbumClick)
    }

    fun addTemporaryMusicBrainzAlbum(releaseGroupId: String, onFinish: (String) -> Unit) {
        launchOnIOThread {
            val releaseGroup = repos.musicBrainz.getReleaseGroup(releaseGroupId)

            repos.musicBrainz.listReleasesByReleaseGroupId(releaseGroupId).firstOrNull()?.also { release ->
                val albumCombo =
                    release.toAlbumWithTracks(isLocal = false, isInLibrary = false, releaseGroup = releaseGroup)
                val album = repos.album.getOrCreateAlbumByMusicBrainzId(albumCombo.album, releaseGroupId, release.id)
                val albumArtists = albumCombo.artists.map { it.withAlbumId(album.albumId) }

                repos.artist.insertAlbumArtists(albumArtists)
                onMainThread { onFinish(album.albumId) }
            }
        }
    }

    fun cancelAlbumDownload(albumId: String) {
        _albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    suspend fun deleteLocalAlbumFiles(albumIds: Collection<String>) {
        deleteLocalAlbumComboFiles(repos.album.listAlbumsWithTracks(albumIds), updateDb = true)
    }

    fun doStartupTasks() {
        launchOnIOThread {
            updateGenreList()
            if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums()
            handleOrphansAndDuplicates()
            repos.playlist.deleteOrphanPlaylistTracks()
            repos.track.deleteTempTracks()
            repos.album.deleteTempAlbums()
            // repos.artist.deleteOrphanArtists()
        }
    }

    fun downloadAlbum(
        albumId: String,
        onFinish: (AlbumDownloadTask.Result) -> Unit,
        onTrackError: (TrackCombo, Throwable) -> Unit,
    ) {
        launchOnIOThread {
            repos.album.getAlbumWithTracks(albumId)?.also { albumCombo ->
                repos.settings
                    .createAlbumDirectory(albumCombo.album.title, albumCombo.artists.joined())
                    ?.also { directory ->
                        val trackCombos = albumCombo.trackCombos
                            .filter { !it.track.isDownloaded && it.track.isOnYoutube }
                        val trackTasks = trackCombos.map { trackCombo ->
                            createTrackDownloadTask(
                                combo = trackCombo,
                                directory = directory,
                                onError = { onTrackError(trackCombo, it) },
                            )
                        }
                        val album = albumCombo.album.copy(isLocal = true, isInLibrary = true)

                        _albumDownloadTasks.value += AlbumDownloadTask(
                            album = album,
                            trackTasks = trackTasks,
                            onFinish = { result ->
                                if (result.succeededTracks.isNotEmpty()) launchOnIOThread {
                                    repos.album.upsertAlbum(album)
                                }
                                onFinish(result)
                            },
                        )
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

    suspend fun ensureTrackMetadata(track: Track, forceReload: Boolean = false, commit: Boolean = true): Track =
        repos.youtube.ensureTrackMetadata(track = track, forceReload = forceReload) {
            if (commit) repos.track.upsertTrack(it)
        }

    fun ensureTrackMetadataAsync(trackId: String) {
        launchOnIOThread {
            repos.track.getTrackById(trackId)?.also { ensureTrackMetadata(track = it) }
        }
    }

    fun exportTracksAsJspf(
        trackCombos: Collection<TrackCombo>,
        outputUri: Uri,
        dateTime: OffsetDateTime,
        title: String? = null,
    ): Boolean {
        val jspf = XSPFPlaylist.fromTrackCombos(combos = trackCombos, title = title, dateTime = dateTime).toJson()

        return context.contentResolver.openAssetFileDescriptor(outputUri, "wt")
            ?.createOutputStream()
            ?.bufferedWriter()
            ?.use {
                it.write(jspf)
                true
            } ?: false
    }

    fun exportTracksAsXspf(
        trackCombos: Collection<TrackCombo>,
        outputUri: Uri,
        dateTime: OffsetDateTime,
        title: String? = null,
    ): Boolean {
        val xspf = XSPFPlaylist.fromTrackCombos(combos = trackCombos, title = title, dateTime = dateTime).toXml()

        return xspf?.let {
            context.contentResolver.openAssetFileDescriptor(outputUri, "wt")
                ?.createOutputStream()
                ?.bufferedWriter()
                ?.use { writer ->
                    writer.write(it)
                    true
                } ?: false
        } ?: false
    }

    fun getAlbumDownloadUiStateFlow(albumId: String) = _albumDownloadTasks
        .flatMapLatest { tasks -> tasks.find { it.album.albumId == albumId }?.uiStateFlow ?: emptyFlow() }

    fun getTrackDownloadUiStateFlow(trackId: String) = _trackDownloadTasks
        .flatMapLatest { tasks -> tasks.find { it.track.trackId == trackId }?.uiStateFlow ?: emptyFlow() }

    suspend fun importNewLocalAlbums() {
        if (!repos.localMedia.isImportingLocalMedia.value) {
            val localMusicDirectory =
                repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

            if (localMusicDirectory != null) {
                repos.localMedia.setIsImporting(true)

                val existingAlbumCombos = repos.album.listAlbumCombos()
                val existingTrackUris = repos.track.listTrackLocalUris()

                for (localAlbum in repos.localMedia.importableAlbumsChannel(localMusicDirectory, existingTrackUris)) {
                    val existingCombo = existingAlbumCombos.find {
                        (it.album.title == localAlbum.title && it.artists.joined() == localAlbum.artistName) ||
                            (it.album.musicBrainzReleaseId != null && it.album.musicBrainzReleaseId == localAlbum.musicBrainzReleaseId)
                    }
                    val combo = localAlbum.toAlbumWithTracks(base = existingCombo).let { combo ->
                        val albumArt = getBestNewLocalAlbumArt(combo.trackCombos)
                            ?: repos.musicBrainz.getCoverArtArchiveImage(
                                combo.album.musicBrainzReleaseId,
                                combo.album.musicBrainzReleaseGroupId
                            )?.toMediaStoreImage()

                        if (albumArt != null) combo.withAlbum(album = combo.album.withAlbumArt(albumArt = albumArt))
                        else combo
                    }

                    upsertAlbumWithTracks(combo)

                    val savedCombo = repos.album.getAlbumWithTracks(combo.album.albumId)

                    savedCombo?.also { updateAlbumFromRemotes(it) }
                }

                repos.localMedia.setIsImporting(false)
            }
        }
    }

    fun matchUnplayableTracks(albumCombo: AlbumWithTracksCombo) = channelFlow<ProgressData> {
        val progressData = ProgressData(text = context.getString(R.string.matching), isActive = true).also { send(it) }
        val unplayableTrackIds = albumCombo.trackCombos.filter { !it.track.isPlayable }.map { it.track.trackId }
        val match = repos.youtube.getBestAlbumMatch(albumCombo) { progress ->
            trySend(progressData.copy(progress = progress * 0.5))
        }
        val matchedCombo = match?.albumCombo
        val updatedTracks = matchedCombo?.trackCombos
            ?.map { it.track }
            ?.filter { unplayableTrackIds.contains(it.trackId) }
            ?: emptyList()

        if (updatedTracks.isNotEmpty()) {
            send(progressData.copy(progress = 0.5, text = context.getString(R.string.importing)))
            repos.track.upsertTracks(updatedTracks.map { ensureTrackMetadata(it, commit = false) })
            send(progressData.copy(progress = 0.9, text = context.getString(R.string.importing)))
        }

        // So youtubePlaylist gets saved:
        matchedCombo?.album?.also { repos.album.upsertAlbum(it) }
        repos.message.onMatchUnplayableTracks(updatedTracks.size)
        send(ProgressData())
    }

    suspend fun updateTrack(
        trackId: String,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumPosition: Int? = null,
        discNumber: Int? = null,
    ) {
        val trackCombo = repos.track.getTrackComboById(trackId)
        val albumCombo = trackCombo?.track?.albumId?.let { repos.album.getAlbumCombo(it) }

        if (trackCombo != null) updateTrack(
            trackCombo = trackCombo,
            title = title,
            year = year,
            artistNames = artistNames,
            albumPosition = albumPosition,
            discNumber = discNumber,
            album = albumCombo?.album,
            albumArtists = albumCombo?.artists,
        )
    }

    suspend fun upsertAlbumWithTracks(combo: IAlbumWithTracksCombo<IAlbum>) {
        database.withTransaction {
            repos.album.upsertAlbum(combo.album)
            repos.album.setAlbumTags(combo.album.albumId, combo.tags)
            repos.track.setAlbumComboTracks(combo)
            repos.artist.setAlbumComboArtists(combo)
        }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun createTrackDownloadTask(
        combo: ITrackCombo,
        directory: DocumentFile,
        onError: (Throwable) -> Unit = {},
    ): TrackDownloadTask {
        val task = TrackDownloadTask(
            scope = scope,
            track = combo.track,
            trackArtists = combo.trackArtists,
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

        if (tracks.isNotEmpty()) repos.track.deleteTrackFiles(tracks)

        if (updateDb && combos.isNotEmpty()) database.withTransaction {
            repos.album.setAlbumsIsLocal(combos.map { it.album.albumId }, false)
            combos.filter { it.album.albumArt?.isLocal == true }.forEach { repos.album.clearAlbumArt(it.album.albumId) }
            if (tracks.isNotEmpty()) repos.track.clearLocalUris(tracks.map { it.trackId })
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
            if (duplicateNonAlbumTracks.isNotEmpty())
                repos.track.deleteTracksById(duplicateNonAlbumTracks.map { it.trackId })
            // Update tracks with broken localUris:
            if (brokenUriTrackIds.isNotEmpty()) repos.track.clearLocalUris(brokenUriTrackIds)
            // Update albums that should have isLocal=true, but don't:
            if (noLongerLocalAlbumsIds.isNotEmpty()) repos.album.setAlbumsIsLocal(noLongerLocalAlbumsIds, false)
        }
    }

    private suspend fun getBestNewLocalAlbumArt(trackCombos: Collection<ITrackCombo>): MediaStoreImage? =
        trackCombos.map { it.track }
            .listCoverImages(context)
            .map { it.uri.toMediaStoreImage() }
            .maxByOrNull { albumArt -> albumArt.fullUri.getBitmap(context)?.getSquareSize() ?: 0 }

    private suspend fun updateAlbumFromMusicBrainz(
        combo: IAlbumWithTracksCombo<IAlbum>,
        trackMergeStrategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_SELF,
        albumArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        trackArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        tagUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE,
    ): UnsavedAlbumWithTracksCombo? {
        return if (combo.album.musicBrainzReleaseId == null) {
            repos.musicBrainz.matchAlbumWithTracks(
                combo = combo,
                trackMergeStrategy = trackMergeStrategy,
                albumArtistUpdateStrategy = albumArtistUpdateStrategy,
                trackArtistUpdateStrategy = trackArtistUpdateStrategy,
                tagUpdateStrategy = tagUpdateStrategy,
            )
        } else null
    }

    private fun updateAlbumFromRemotes(combo: IAlbumWithTracksCombo<IAlbum>) {
        launchOnIOThread {
            val spotifyCombo = updateAlbumFromSpotify(combo)
            val mbCombo = updateAlbumFromMusicBrainz(spotifyCombo ?: combo)

            upsertAlbumWithTracks(mbCombo ?: spotifyCombo ?: combo)
        }
    }

    private suspend fun updateAlbumFromSpotify(
        combo: IAlbumWithTracksCombo<IAlbum>,
        trackMergeStrategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_SELF,
        albumArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        trackArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        tagUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE,
    ): UnsavedAlbumWithTracksCombo? {
        return if (combo.album.spotifyId == null) {
            repos.spotify.matchAlbumWithTracks(
                combo = combo,
                trackMergeStrategy = trackMergeStrategy,
                albumArtistUpdateStrategy = albumArtistUpdateStrategy,
                trackArtistUpdateStrategy = trackArtistUpdateStrategy,
                tagUpdateStrategy = tagUpdateStrategy,
            )
        } else null
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

    private suspend fun updateTrack(
        trackCombo: ITrackCombo,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumPosition: Int? = null,
        discNumber: Int? = null,
        album: IAlbum? = null,
        albumArtists: Collection<IArtistCredit>? = null,
    ) {
        var finalTrackArtists = trackCombo.trackArtists.toList()

        if (artistNames.filter { it.isNotEmpty() } != trackCombo.trackArtists.map { it.name }) {
            finalTrackArtists = artistNames.filter { it.isNotEmpty() }.mapIndexed { index, name ->
                UnsavedTrackArtistCredit(
                    name = name,
                    trackId = trackCombo.track.trackId,
                    position = index,
                )
            }

            repos.artist.setTrackArtists(trackCombo.track.trackId, finalTrackArtists)
        }

        val updatedTrack = ensureTrackMetadata(
            track = trackCombo.track.copy(
                title = title,
                year = year,
                albumPosition = albumPosition ?: trackCombo.track.albumPosition,
                discNumber = discNumber ?: trackCombo.track.discNumber,
            ),
            commit = false,
        )

        repos.track.upsertTrack(updatedTrack)
        repos.localMedia.tagTrack(
            track = updatedTrack,
            trackArtists = finalTrackArtists,
            album = album,
            albumArtists = albumArtists,
        )
    }
}
