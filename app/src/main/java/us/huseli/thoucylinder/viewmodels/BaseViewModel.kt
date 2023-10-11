package us.huseli.thoucylinder.viewmodels

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID

abstract class BaseViewModel(private val repos: Repositories) : ViewModel() {
    private val _selectedAlbums = MutableStateFlow<List<Album>>(emptyList())
    private val _selectedTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _trackDownloadProgressMap = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())

    val selectedAlbums = _selectedAlbums.asStateFlow()
    val selectedTracks = _selectedTracks.asStateFlow()
    val trackDownloadProgressMap = _trackDownloadProgressMap.asStateFlow()

    fun addPlaylist(playlist: Playlist, selection: Selection? = null) = viewModelScope.launch(Dispatchers.IO) {
        repos.local.insertPlaylist(playlist, selection)
    }

    fun downloadTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        try {
            var newTrack = repos.youtube.downloadTrack(
                track = track,
                progressCallback = {
                    _trackDownloadProgressMap.value += track.trackId to it.copy(progress = it.progress * 0.8)
                }
            )
            newTrack = repos.mediaStore.moveTaggedTrackToMediaStore(newTrack) {
                _trackDownloadProgressMap.value += track.trackId to it.copy(progress = 0.8 + (it.progress * 0.2))
            }
            repos.local.insertTrack(newTrack)
        } catch (e: Exception) {
            Log.e("downloadTrack", e.toString(), e)
        } finally {
            _trackDownloadProgressMap.value -= track.trackId
        }
    }

    suspend fun getImageBitmap(image: Image): ImageBitmap? = repos.local.getImageBitmap(image)

    suspend fun getTrackMetadata(track: Track): TrackMetadata? {
        if (track.metadata != null) return track.metadata
        val youtubeMetadata =
            track.youtubeVideo?.metadata ?: withContext(Dispatchers.IO) { repos.youtube.getBestMetadata(track) }
        return youtubeMetadata?.toTrackMetadata()
    }

    fun playAlbum(albumId: UUID) = playAlbums(listOf(albumId))

    fun playAlbums(albumIds: List<UUID>) = viewModelScope.launch(Dispatchers.IO) {
        repos.player.playAlbums(albumIds.mapNotNull { repos.local.getAlbumWithTracks(it) })
    }

    fun playTrack(track: Track) = repos.player.playTrack(track)

    fun toggleSelected(album: Album) {
        if (_selectedAlbums.value.contains(album))
            _selectedAlbums.value -= album
        else
            _selectedAlbums.value += album
    }

    fun toggleSelected(track: Track) {
        if (_selectedTracks.value.contains(track))
            _selectedTracks.value -= track
        else
            _selectedTracks.value += track
    }

    fun unselectAllAlbums() {
        _selectedAlbums.value = emptyList()
    }

    fun unselectAllTracks() {
        _selectedTracks.value = emptyList()
    }
}
