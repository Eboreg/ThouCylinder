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
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    private val _albumThumbnailCache = mutableMapOf<UUID, ImageBitmap>()
    private val _trackThumbnailCache = mutableMapOf<UUID, ImageBitmap>()

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
        pojo: AlbumWithTracksPojo,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): AlbumWithTracksPojo = pojo.copy(
        tracks = pojo.tracks.map { track ->
            ensureTrackMetadata(track, commit = commit, forceReload = forceReload)
        }
    )

    fun ensureAlbumTracksMetadataAsync(
        pojo: AlbumWithTracksPojo,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (AlbumWithTracksPojo) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureAlbumTracksMetadata(pojo, commit, forceReload))
    }

    fun ensureAlbumTracksMetadataAsync(
        pojos: Collection<AlbumWithTracksPojo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (List<AlbumWithTracksPojo>) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureAlbumTracksMetadata(pojos, commit, forceReload))
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

    fun ensureTrackPojoMetadataAsync(
        pojo: TrackPojo,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (TrackPojo) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureTrackPojoMetadata(pojo, commit, forceReload))
    }

    fun ensureTrackPojoMetadataAsync(
        pojos: List<TrackPojo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
        callback: (List<TrackPojo>) -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        callback(ensureTrackPojoMetadata(pojos, commit, forceReload))
    }

    suspend fun getTrackThumbnail(
        track: Track,
        albumPojo: AbstractAlbumPojo? = null,
        album: Album? = albumPojo?.album,
        context: Context,
    ): ImageBitmap? {
        _trackThumbnailCache[track.trackId]?.also { return it }
        track.getThumbnail(context)?.also {
            _trackThumbnailCache += track.trackId to it
            return it
        }

        if (album != null) {
            _albumThumbnailCache[album.albumId]?.also { return it }
            album.getThumbnail(context)?.also {
                _albumThumbnailCache += album.albumId to it
                return it
            }

            val spotifyAlbum =
                if (albumPojo != null) albumPojo.spotifyAlbum
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
                    val newAlbumPojos =
                        repos.localMedia.listNewLocalAlbums(localMusicDirectory, existingTrackUris)

                    newAlbumPojos.forEach { pojo -> repos.album.saveAlbumPojo(pojo) }
                    newAlbumPojos.flatMap { it.tracks }.also { repos.track.insertTracks(it) }
                }

                repos.localMedia.setIsImporting(false)
            }
        }

    fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.saveAlbumPojo(pojo.copy(album = pojo.album.copy(isInLibrary = true)))
        repos.track.deleteTracksByAlbumId(pojo.album.albumId)
        repos.track.insertTracks(pojo.tracks.map { it.copy(albumId = pojo.album.albumId, isInLibrary = true) })
        repos.localMedia.getOrCreateAlbumArt(pojo)
    }


    /** PRIVATE METHODS ***********************************************************************************************/
    private suspend fun ensureAlbumTracksMetadata(
        pojos: Collection<AlbumWithTracksPojo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): List<AlbumWithTracksPojo> = pojos.map { ensureAlbumTracksMetadata(it, commit, forceReload) }

    private suspend fun ensureTrackPojoMetadata(
        pojo: TrackPojo,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): TrackPojo = pojo.copy(track = ensureTrackMetadata(pojo.track, commit, forceReload))

    private suspend fun ensureTrackPojoMetadata(
        pojos: List<TrackPojo>,
        commit: Boolean = true,
        forceReload: Boolean = false,
    ): List<TrackPojo> = pojos.map { pojo -> ensureTrackPojoMetadata(pojo, commit, forceReload) }
}
