package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    val trackProgressDataMap = repos.youtube.trackProgressDataMap

    fun enqueueAlbum(album: Album, context: Context) = enqueueAlbums(listOf(album), context)

    fun enqueueAlbumPojos(pojos: List<AbstractAlbumPojo>, context: Context) =
        enqueueAlbums(pojos.map { it.album }, context)

    fun enqueueAlbums(albums: List<Album>, context: Context) = viewModelScope.launch {
        val pojos =
            getQueueTrackPojos(repos.room.listAlbumTrackPojos(albums.map { it.albumId }), repos.player.nextItemIndex)

        if (pojos.isNotEmpty()) {
            repos.player.insertNext(pojos)
            SnackbarEngine.addInfo(
                context.resources.getQuantityString(R.plurals.x_albums_enqueued_next, albums.size, albums.size)
            )
        }
    }

    fun enqueueTrackPojo(pojo: AbstractTrackPojo, context: Context) = enqueueTrackPojos(listOf(pojo), context)

    fun enqueueTrackPojos(pojos: List<AbstractTrackPojo>, context: Context) = viewModelScope.launch {
        repos.player.insertNext(getQueueTrackPojos(pojos, repos.player.nextItemIndex))
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, pojos.size, pojos.size)
        )
    }

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

    private suspend fun getQueueTrackPojo(trackPojo: AbstractTrackPojo, index: Int): QueueTrackPojo? {
        val track = ensureTrackMetadata(trackPojo.track, commit = true)
        return track.playUri?.let { uri ->
            QueueTrackPojo(track = track, uri = uri, position = index, album = trackPojo.album)
        }
    }

    private suspend fun getQueueTrackPojos(
        trackPojos: List<AbstractTrackPojo>,
        startIndex: Int = 0,
    ): List<QueueTrackPojo> {
        var offset = 0
        return trackPojos.mapNotNull { pojo -> getQueueTrackPojo(pojo, startIndex + offset)?.also { offset++ } }
    }

    fun playAlbum(album: Album) = playAlbums(listOf(album))

    fun playAlbumPojos(pojos: List<AbstractAlbumPojo>) = playAlbums(pojos.map { it.album })

    fun playAlbums(albums: List<Album>) = viewModelScope.launch {
        repos.player.replaceAndPlay(
            getQueueTrackPojos(repos.room.listAlbumTrackPojos(albums.map { it.albumId }))
        )
    }

    fun playPlaylist(playlistId: UUID, startTrackId: UUID? = null) = viewModelScope.launch {
        val pojos = getQueueTrackPojos(repos.room.listPlaylistTracks(playlistId))
        val startIndex =
            startTrackId?.let { trackId -> pojos.indexOfFirst { it.trackId == trackId }.takeIf { it > -1 } } ?: 0
        repos.player.replaceAndPlay(pojos, startIndex = startIndex)
    }

    fun playTrackPojo(pojo: AbstractTrackPojo) = viewModelScope.launch {
        getQueueTrackPojo(pojo, repos.player.nextItemIndex)?.also { repos.player.insertNextAndPlay(it) }
    }

    fun playTrackPojos(pojos: List<AbstractTrackPojo>, startIndex: Int = 0) = viewModelScope.launch {
        repos.player.replaceAndPlay(getQueueTrackPojos(pojos), startIndex)
    }
}
