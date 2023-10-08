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
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.MediaStoreRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.util.UUID

abstract class BaseViewModel(
    private val repo: LocalRepository,
    private val playerRepo: PlayerRepository,
    private val youtubeRepo: YoutubeRepository,
    private val mediaStoreRepo: MediaStoreRepository,
) : ViewModel() {
    private val _selection = MutableStateFlow(Selection())
    private val _trackDownloadProgressMap = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())

    val playerPlaybackState = playerRepo.playbackState
    val playerPlayingTrack = playerRepo.playingTrack
    val playerCurrentTrack = playerRepo.currentTrack
    val selection = _selection.asStateFlow()
    val trackDownloadProgressMap = _trackDownloadProgressMap.asStateFlow()

    fun downloadTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        try {
            var newTrack = youtubeRepo.downloadTrack(
                track = track,
                progressCallback = {
                    _trackDownloadProgressMap.value += track.trackId to it.copy(progress = it.progress * 0.8)
                }
            )
            newTrack = mediaStoreRepo.moveTaggedTrackToMediaStore(newTrack) {
                _trackDownloadProgressMap.value += track.trackId to it.copy(progress = 0.8 + (it.progress * 0.2))
            }
            repo.insertTrack(newTrack)
        } catch (e: Exception) {
            Log.e("downloadTrack", e.toString(), e)
        } finally {
            _trackDownloadProgressMap.value -= track.trackId
        }
    }

    suspend fun getImageBitmap(image: Image): ImageBitmap? = repo.getImageBitmap(image)

    suspend fun getTrackMetadata(track: Track): TrackMetadata? {
        if (track.metadata != null) return track.metadata
        val youtubeMetadata =
            track.youtubeVideo?.metadata ?: withContext(Dispatchers.IO) { youtubeRepo.getBestMetadata(track) }
        return youtubeMetadata?.toTrackMetadata()
    }

    fun playOrPause(track: Track) = playerRepo.playOrPause(track)

    fun toggleTrackSelected(track: Track) {
        _selection.value = _selection.value.toggleTrackSelected(track)
    }

    fun unselectAllTracks() {
        _selection.value = _selection.value.copy(tracks = emptyList())
    }
}
