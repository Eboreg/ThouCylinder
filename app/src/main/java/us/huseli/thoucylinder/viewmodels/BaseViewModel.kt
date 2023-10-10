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
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.entities.AbstractQueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID

abstract class BaseViewModel(private val repos: Repositories) : ViewModel() {
    private val _selection = MutableStateFlow(Selection())
    private val _trackDownloadProgressMap = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())

    val selection = _selection.asStateFlow()
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

    fun play(track: Track) = repos.player.playTrack(track)

    fun toggleSelected(track: Track) {
        _selection.value = _selection.value.toggleSelected(track)
    }

    fun toggleSelected(queueTrack: AbstractQueueTrack) {
        _selection.value = _selection.value.toggleSelected(queueTrack)
    }

    fun unselectAllTracks() {
        _selection.value = _selection.value.copy(tracks = emptyList())
    }
}
