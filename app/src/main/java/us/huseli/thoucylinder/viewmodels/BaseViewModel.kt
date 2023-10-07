package us.huseli.thoucylinder.viewmodels

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.util.UUID

abstract class BaseViewModel(
    private val playerRepo: PlayerRepository,
    private val repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
) : ViewModel() {
    private val _selection = MutableStateFlow(Selection())
    private val _trackDownloadProgressMap = MutableStateFlow<Map<UUID, DownloadProgress>>(emptyMap())

    val playerPlaybackState = playerRepo.playbackState
    val playerPlayingTrack = playerRepo.playingTrack
    val playerCurrentTrack = playerRepo.currentTrack
    val selection = _selection.asStateFlow()
    val trackDownloadProgressMap = _trackDownloadProgressMap.asStateFlow()

    fun downloadTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        track.youtubeVideo?.let { video ->
            try {
                var newTrack = youtubeRepo.downloadTrack(
                    video = video,
                    progressCallback = {
                        _trackDownloadProgressMap.value += track.trackId to it.copy(progress = it.progress * 0.8)
                    },
                )
                newTrack = repo.moveTaggedTrackToMediaStore(newTrack) {
                    _trackDownloadProgressMap.value += track.trackId to it.copy(progress = 0.8 + (it.progress * 0.2))
                }
                repo.insertTrack(newTrack)
            } catch (e: Exception) {
                Log.e("downloadTrack", e.toString(), e)
            } finally {
                _trackDownloadProgressMap.value -= track.trackId
            }
        }
    }

    suspend fun getImageBitmap(image: Image): ImageBitmap? = repo.getImageBitmap(image)

    fun playOrPause(track: Track) = playerRepo.playOrPause(track)

    fun toggleTrackSelected(track: Track) {
        _selection.value = _selection.value.toggleTrackSelected(track)
    }

    fun unselectAllTracks() {
        _selection.value = _selection.value.copy(tracks = emptyList())
    }
}
