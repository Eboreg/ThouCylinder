package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    val trackDownloadProgressMap = repos.youtube.trackDownloadProgressMap

    fun enqueueAlbum(album: Album, context: Context) = enqueueAlbums(listOf(album), context)

    fun enqueueAlbumPojos(pojos: List<AbstractAlbumPojo>, context: Context) =
        enqueueAlbums(pojos.map { it.album }, context)

    fun enqueueAlbums(albums: List<Album>, context: Context) = viewModelScope.launch {
        repos.player.insertNext(repos.room.listAlbumTrackPojos(albums.map { it.albumId }))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_albums_enqueued_next, albums.size, albums.size)
        )
    }

    fun enqueueTrackPojo(pojo: AbstractTrackPojo, context: Context) = enqueueTrackPojos(listOf(pojo), context)

    fun enqueueTrackPojos(pojos: List<AbstractTrackPojo>, context: Context) {
        repos.player.insertNext(pojos)
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, pojos.size, pojos.size)
        )
    }

    suspend fun getTrackMetadata(track: Track): TrackMetadata? {
        if (track.metadata != null) return track.metadata
        val youtubeMetadata = track.youtubeVideo?.metadata ?: repos.youtube.getBestMetadata(track)
        return youtubeMetadata?.toTrackMetadata()
    }

    fun playAlbum(album: Album) = playAlbums(listOf(album))

    fun playAlbumPojos(pojos: List<AbstractAlbumPojo>) = playAlbums(pojos.map { it.album })

    fun playAlbums(albums: List<Album>) = viewModelScope.launch {
        repos.player.replaceAndPlay(repos.room.listAlbumTrackPojos(albums.map { it.albumId }))
    }

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = viewModelScope.launch {
        val pojos = repos.room.listPlaylistTracks(playlistId)
        val startIndex =
            startTrackId?.let { trackId -> pojos.indexOfFirst { it.trackId == trackId }.takeIf { it > -1 } } ?: 0
        repos.player.replaceAndPlay(trackPojos = pojos, startIndex = startIndex)
    }

    fun playTrackPojo(pojo: AbstractTrackPojo) = repos.player.insertNextAndPlay(pojo)

    fun playTrackPojos(pojos: List<AbstractTrackPojo>, startIndex: Int? = 0) =
        repos.player.replaceAndPlay(pojos, startIndex)

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
