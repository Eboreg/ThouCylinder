package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.ContextMutexCache
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    private val _albumThumbnailCache = mutableMapOf<UUID, ImageBitmap>()
    private val _trackThumbnailCache = ContextMutexCache<Track, UUID, ImageBitmap>(
        keyFromInstance = { track -> track.trackId },
        fetchMethod = { track, context -> track.getThumbnail(context) },
    )

    val totalAreaSize: Flow<DpSize> =
        combine(repos.settings.contentAreaSize, repos.settings.innerPadding) { size, padding -> // including menu
            size.plus(
                DpSize(
                    padding.calculateLeftPadding(LayoutDirection.Ltr) + padding.calculateRightPadding(LayoutDirection.Ltr),
                    padding.calculateTopPadding() + padding.calculateBottomPadding(),
                )
            )
        }

    suspend fun ensureAlbumTracksMetadata(
        combo: AlbumWithTracksCombo,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): AlbumWithTracksCombo = combo.copy(
        tracks = combo.tracks.map { track ->
            ensureTrackMetadata(track, commit = commit, forceReload = forceReload)
        }
    )

    fun ensureAlbumTracksMetadataAsync(
        combo: AlbumWithTracksCombo,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (AlbumWithTracksCombo) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureAlbumTracksMetadata(combo, commit, forceReload))
    }

    fun ensureAlbumTracksMetadataAsync(
        combos: Collection<AlbumWithTracksCombo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (List<AlbumWithTracksCombo>) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureAlbumTracksMetadata(combos, commit, forceReload))
    }

    suspend fun ensureTrackMetadata(track: Track, commit: Boolean = true, forceReload: Boolean = false): Track {
        var changed = false
        val youtubeMetadata =
            if (track.youtubeVideo?.metadata == null || forceReload)
                repos.youtube.getBestMetadata(track, forceReload)?.also { changed = true }
            else track.youtubeVideo.metadata
        val metadata =
            if (track.metadata == null || forceReload) youtubeMetadata?.toTrackMetadata()?.also { changed = true }
            else track.metadata

        if (!changed) return track

        return track.copy(
            metadata = metadata,
            youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
        ).also { if (commit) repos.track.updateTrack(it) }
    }

    fun ensureTrackComboMetadataAsync(
        combo: TrackCombo,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (TrackCombo) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureTrackComboMetadata(combo, commit, forceReload))
    }

    fun ensureTrackComboMetadataAsync(
        combos: List<TrackCombo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (List<TrackCombo>) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureTrackComboMetadata(combos, commit, forceReload))
    }

    suspend fun getTrackThumbnail(
        track: Track,
        albumCombo: AbstractAlbumCombo? = null,
        album: Album? = albumCombo?.album,
        context: Context,
    ): ImageBitmap? {
        _trackThumbnailCache.get(track, context)?.also { return it }

        if (album != null) {
            _albumThumbnailCache[album.albumId]?.also { return it }
            album.getThumbnail(context)?.also {
                _albumThumbnailCache += album.albumId to it
                return it
            }

            val spotifyAlbum =
                if (albumCombo != null) albumCombo.spotifyAlbum
                else repos.spotify.getSpotifyAlbum(album.albumId)

            spotifyAlbum?.getThumbnailImageBitmap(context)?.also {
                _albumThumbnailCache += album.albumId to it
                return it
            }
        }

        return null
    }

    fun importNewLocalAlbums(context: Context, existingTracks: List<Track>? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            if (!repos.localMedia.isImportingLocalMedia.value) {
                repos.localMedia.setIsImporting(true)

                val localMusicDirectory =
                    repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }
                val existingTrackUris = existingTracks?.mapNotNull { it.localUri } ?: repos.track.listTrackLocalUris()

                if (localMusicDirectory != null) {
                    val newAlbumCombos =
                        repos.localMedia.listNewLocalAlbums(localMusicDirectory, existingTrackUris)

                    repos.album.saveAlbumCombos(newAlbumCombos)
                    newAlbumCombos.flatMap { it.tracks }.also { repos.track.insertTracks(it) }
                }

                repos.localMedia.setIsImporting(false)
            }
        }

    fun saveAlbumWithTracks(combo: AlbumWithTracksCombo) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.saveAlbumCombo(combo.copy(album = combo.album.copy(isInLibrary = true)))
        repos.track.deleteTracksByAlbumId(combo.album.albumId)
        repos.track.insertTracks(combo.tracks.map { it.copy(albumId = combo.album.albumId, isInLibrary = true) })
        repos.localMedia.getOrCreateAlbumArt(combo)
    }

    fun saveTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) { repos.track.updateTrack(track) }


    /** PRIVATE METHODS ***********************************************************************************************/
    private suspend fun ensureAlbumTracksMetadata(
        combos: Collection<AlbumWithTracksCombo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): List<AlbumWithTracksCombo> = combos.map { ensureAlbumTracksMetadata(it, commit, forceReload) }

    private suspend fun ensureTrackComboMetadata(
        combo: TrackCombo,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): TrackCombo = combo.copy(track = ensureTrackMetadata(combo.track, commit, forceReload))

    private suspend fun ensureTrackComboMetadata(
        combos: List<TrackCombo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): List<TrackCombo> = combos.map { combo -> ensureTrackComboMetadata(combo, commit, forceReload) }
}
