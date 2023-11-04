package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    val trackDownloadProgressMap = repos.youtube.trackDownloadProgressMap

    suspend fun getTrackThumbnail(pojo: TrackPojo, context: Context): ImageBitmap? =
        pojo.track.getThumbnail(context)?.asImageBitmap()
            ?: pojo.album?.getThumbnail(context)?.asImageBitmap()

    suspend fun getTrackFullImage(pojo: TrackPojo, context: Context): ImageBitmap? =
        pojo.track.getFullImage(context)?.asImageBitmap()
            ?: pojo.album?.getFullImage(context)?.asImageBitmap()

    suspend fun getTrackMetadata(track: Track): TrackMetadata? {
        if (track.metadata != null) return track.metadata
        val youtubeMetadata = track.youtubeVideo?.metadata ?: repos.youtube.getBestMetadata(track)
        return youtubeMetadata?.toTrackMetadata()
    }

    fun playAlbum(album: Album) = playAlbums(listOf(album))

    fun playAlbumNext(album: Album, context: Context) = playAlbumsNext(listOf(album), context)

    fun playAlbums(albums: List<Album>) = viewModelScope.launch {
        val pojos = repos.room.listAlbumTrackPojos(albums.map { it.albumId })
        repos.player.replaceAndPlay(pojos)
    }

    fun playAlbumsNext(albums: List<Album>, context: Context) = viewModelScope.launch {
        repos.player.insertNext(repos.room.listAlbumTrackPojos(albums.map { it.albumId }))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_albums_enqueued_next, albums.size, albums.size)
        )
    }

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = viewModelScope.launch {
        val pojos = repos.room.listPlaylistTracks(playlistId)
        val startIndex =
            startTrackId?.let { trackId -> pojos.indexOfFirst { it.trackId == trackId }.takeIf { it > -1 } } ?: 0
        repos.player.replaceAndPlay(trackPojos = pojos, startIndex = startIndex)
    }

    fun playTrackPojo(pojo: TrackPojo) = repos.player.insertNextAndPlay(pojo)

    fun playTrackPojoNext(pojo: TrackPojo, context: Context) = playTrackPojosNext(listOf(pojo), context)

    fun playTrackPojos(pojos: List<TrackPojo>) = repos.player.replaceAndPlay(pojos)

    fun playTrackPojosNext(pojos: List<TrackPojo>, context: Context) {
        repos.player.insertNext(pojos)
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, pojos.size, pojos.size)
        )
    }

    fun removeAlbumFromLibrary(album: Album) = viewModelScope.launch {
        repos.room.deleteAlbumWithTracks(album)
    }

    protected suspend fun ensureTrackMetadata(track: Track): Track {
        val youtubeMetadata = track.youtubeVideo?.metadata ?: repos.youtube.getBestMetadata(track)
        val metadata = track.metadata ?: youtubeMetadata?.toTrackMetadata()

        return track.copy(
            metadata = metadata,
            youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
        )
    }
}
