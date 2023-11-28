package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    private val _albumThumbnailCache = mutableMapOf<UUID, ImageBitmap>()
    private val _trackThumbnailCache = mutableMapOf<UUID, ImageBitmap>()

    suspend fun ensureTrackMetadata(track: Track, commit: Boolean, forceReload: Boolean = false): Track {
        var changed = false
        val youtubeMetadata = track.youtubeVideo?.metadata
            ?: repos.youtube.getBestMetadata(track, forceReload = forceReload)?.also { changed = true }
        val metadata = track.metadata ?: youtubeMetadata?.toTrackMetadata()?.also { changed = true }

        if (!changed) return track

        return track.copy(
            metadata = metadata,
            youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
        ).also { if (commit) repos.room.updateTrack(it) }
    }

    suspend fun getTrackThumbnail(track: Track, album: Album?, context: Context): ImageBitmap? {
        val trackThumbnail = _trackThumbnailCache[track.trackId]
            ?: track.getThumbnail(context)?.also { _trackThumbnailCache += track.trackId to it }

        if (trackThumbnail != null) return trackThumbnail

        if (album != null) return _albumThumbnailCache[album.albumId]
            ?: album.getThumbnail(context)?.also { _albumThumbnailCache += album.albumId to it }

        return null
    }

    suspend fun getTrackThumbnail(track: Track, albumPojo: AbstractAlbumPojo?, context: Context): ImageBitmap? =
        getTrackThumbnail(track = track, album = albumPojo?.album, context = context)
            ?: albumPojo?.spotifyAlbum?.thumbnail?.getImageBitmap()
                ?.also { _albumThumbnailCache += albumPojo.album.albumId to it }

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = viewModelScope.launch {
        val pojos = getQueueTrackPojos(repos.room.listPlaylistTracks(playlistId))
        val startIndex =
            startTrackId?.let { trackId -> pojos.indexOfFirst { it.trackId == trackId }.takeIf { it > -1 } } ?: 0
        repos.player.replaceAndPlay(pojos, startIndex = startIndex)
    }

    protected suspend fun getQueueTrackPojos(
        trackPojos: List<AbstractTrackPojo>,
        startIndex: Int = 0,
    ): List<QueueTrackPojo> {
        var offset = 0
        return trackPojos.mapNotNull { pojo -> getQueueTrackPojo(pojo, startIndex + offset)?.also { offset++ } }
    }

    protected suspend fun getQueueTrackPojo(trackPojo: AbstractTrackPojo, index: Int): QueueTrackPojo? {
        val track = ensureTrackMetadata(trackPojo.track, commit = true)
        return track.playUri?.let { uri ->
            QueueTrackPojo(track = track, uri = uri, position = index, album = trackPojo.album)
        }
    }
}
