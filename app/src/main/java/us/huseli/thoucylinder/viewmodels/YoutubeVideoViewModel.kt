package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class YoutubeVideoViewModel @Inject constructor(
    private val repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
    private val playerRepo: PlayerRepository,
) : ViewModel() {
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    private val _metadata = MutableStateFlow<YoutubeMetadata?>(null)
    private val _track = MutableStateFlow<Track?>(null)
    private val _uri = MutableStateFlow<Uri?>(null)
    private val _video = MutableStateFlow<YoutubeVideo?>(null)

    val downloadProgress = _downloadProgress.asStateFlow()
    val isDownloaded: Flow<Boolean> = _track.map { it?.localFile != null }
    val isPlaying: Flow<Boolean> =
        combine(playerRepo.currentUri.filterNotNull(), playerRepo.isPlaying) { uri, isPlaying ->
            isPlaying && uri == _uri.value
        }
    val metadata = _metadata.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _video.filterNotNull().distinctUntilChanged().collect { video ->
                _metadata.value = youtubeRepo.getBestMetadata(video.id)
            }
        }

        viewModelScope.launch {
            combine(_video, repo.tracks) { video, tracks ->
                video?.let { tracks.find { track -> track.youtubeVideo?.id == it.id } }
            }.distinctUntilChanged().collect {
                _track.value = it
            }
        }

        viewModelScope.launch {
            combine(_metadata, _track) { metadata, track -> repo.getLocalTrackUri(track) ?: metadata?.url?.toUri() }
                .distinctUntilChanged()
                .collect { uri -> _uri.value = uri }
        }
    }

    fun download() = viewModelScope.launch(Dispatchers.IO) {
        _video.value?.let { video ->
            try {
                val track = youtubeRepo.downloadTrack(
                    video = video,
                    statusCallback = { _downloadProgress.value = it.copy(progress = it.progress * 0.8) },
                )
                repo.moveAndInsertTrack(track) {
                    _downloadProgress.value = it.copy(progress = 0.8 + (it.progress * 0.2))
                }
            } catch (e: Exception) {
                Log.e("downloadTrack", e.toString(), e)
            } finally {
                _downloadProgress.value = null
            }
        }
    }

    fun playOrPause() = viewModelScope.launch {
        _uri.value?.let { uri -> playerRepo.playOrPause(uri) }
    }

    fun setVideo(value: YoutubeVideo) {
        _video.value = value
    }
}