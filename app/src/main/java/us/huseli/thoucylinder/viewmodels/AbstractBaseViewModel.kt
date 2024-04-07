package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.ILogger
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel(), ILogger {
    fun deactivateRadio() = repos.player.deactivateRadio()

    suspend fun ensureTrackMetadata(track: Track, forceReload: Boolean = false, commit: Boolean = true): Track =
        withContext(Dispatchers.IO) {
            repos.youtube.ensureTrackMetadata(track = track, forceReload = forceReload) {
                if (commit) repos.track.updateTrack(it)
            }
        }

    fun ensureTrackMetadataAsync(track: Track, forceReload: Boolean = false) = launchOnIOThread {
        ensureTrackMetadata(track, forceReload)
    }

    suspend fun getAlbumFullImage(uri: Uri?): ImageBitmap? = repos.album.getFullImage(uri)

    suspend fun getAlbumThumbnail(uri: Uri?): ImageBitmap? = uri?.let { repos.album.thumbnailCache.getOrNull(it) }

    fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    suspend fun getTrackComboFullImage(trackCombo: AbstractTrackCombo): ImageBitmap? =
        getTrackFullImage(trackCombo.track.image?.fullUri)
            ?: getAlbumFullImage(trackCombo.album?.albumArt?.fullUri)

    suspend fun getTrackComboThumbnail(trackCombo: AbstractTrackCombo): ImageBitmap? =
        getTrackThumbnail(trackCombo.track.image?.thumbnailUri)
            ?: getAlbumThumbnail(trackCombo.album?.albumArt?.thumbnailUri)

    suspend fun getTrackFullImage(uri: Uri?): ImageBitmap? = repos.track.getFullImage(uri)

    suspend fun getTrackThumbnail(uri: Uri?): ImageBitmap? = uri?.let { repos.track.thumbnailCache.getOrNull(it) }

    suspend fun importNewLocalAlbums(context: Context) {
        if (!repos.localMedia.isImportingLocalMedia.value) {
            val localMusicDirectory =
                repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

            if (localMusicDirectory != null) {
                repos.localMedia.setIsImporting(true)

                repos.localMedia.importNewLocalAlbums(
                    treeDocumentFile = localMusicDirectory,
                    existingTrackUris = repos.track.listTrackLocalUris(),
                    onEach = { combo ->
                        launchOnIOThread { updateFromMusicBrainz(combo, TrackMergeStrategy.KEEP_SELF) }
                    },
                    getArtist = { repos.artist.artistCache.getByName(it) },
                    existingAlbumsCombos = repos.album.listAlbumCombos(),
                )
                repos.localMedia.setIsImporting(false)
            }
        }
    }

    fun importNewLocalAlbumsAsync(context: Context) = launchOnIOThread { importNewLocalAlbums(context) }

    fun saveTrack(track: Track) = launchOnIOThread { repos.track.updateTrack(track) }

    suspend fun updateFromMusicBrainz(
        combo: AlbumWithTracksCombo,
        strategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
    ) {
        if (combo.album.musicBrainzReleaseId == null) {
            val match = repos.musicBrainz.matchAlbumWithTracks(
                combo = combo,
                strategy = strategy,
                getArtist = { repos.artist.artistCache.get(it) },
            )

            if (match != null) {
                repos.album.updateAlbum(match.album)
                repos.album.setAlbumTags(match.album.albumId, match.tags)
                repos.artist.setAlbumArtists(match.artists.toAlbumArtists())
                repos.track.setAlbumTracks(match.album.albumId, match.trackCombos.map { it.track })
                repos.artist.setTrackArtists(match.trackCombos.flatMap { it.artists.toTrackArtists() })
                match.album.musicBrainzReleaseId?.also { releaseId ->
                    repos.musicBrainz.getReleaseCoverArt(releaseId)?.also {
                        repos.album.updateAlbumArt(match.album.albumId, it)
                    }
                }
            }
        }
    }

    protected suspend fun getQueueTrackComboByViewState(
        state: Track.ViewState,
        matchIfNeeded: Boolean = false,
    ): QueueTrackCombo? = withContext(Dispatchers.IO) {
        getQueueTrackCombo(
            track = state.track,
            album = state.album,
            albumArtists = state.albumArtists,
            trackArtists = state.trackArtists,
            matchIfNeeded = matchIfNeeded,
        )
    }

    protected suspend fun getQueueTrackCombo(
        trackCombo: AbstractTrackCombo,
        matchIfNeeded: Boolean = false,
    ): QueueTrackCombo? = withContext(Dispatchers.IO) {
        getQueueTrackCombo(
            track = trackCombo.track,
            album = trackCombo.album,
            albumArtists = trackCombo.albumArtists,
            trackArtists = trackCombo.artists,
            matchIfNeeded = matchIfNeeded,
        )
    }

    protected suspend fun getQueueTrackCombosByViewState(
        states: Collection<Track.ViewState>,
        matchIfNeeded: Boolean = false,
    ): List<QueueTrackCombo> = states.mapNotNull {
        getQueueTrackComboByViewState(state = it, matchIfNeeded = matchIfNeeded)
    }

    protected suspend fun getQueueTrackCombos(
        trackCombos: Collection<AbstractTrackCombo>,
        matchIfNeeded: Boolean = false,
    ): List<QueueTrackCombo> = trackCombos.mapNotNull {
        getQueueTrackCombo(trackCombo = it, matchIfNeeded = matchIfNeeded)
    }

    private suspend fun getQueueTrackCombo(
        track: Track,
        album: Album? = null,
        albumArtists: Collection<AlbumArtistCredit>,
        trackArtists: Collection<TrackArtistCredit>,
        matchIfNeeded: Boolean = false,
    ): QueueTrackCombo? = withContext(Dispatchers.IO) {
        val newTrack = repos.youtube.ensureTrackPlayUriOrNull(
            track = track,
            albumArtists = albumArtists,
            trackArtists = trackArtists,
            matchIfNeeded = matchIfNeeded,
            onChanged = { repos.track.updateTrack(it) },
        )

        newTrack?.playUri?.let { uri ->
            QueueTrackCombo(
                track = newTrack,
                uri = uri,
                album = album,
                albumArtists = albumArtists.toList(),
                artists = trackArtists.toList(),
            )
        }
    }
}
