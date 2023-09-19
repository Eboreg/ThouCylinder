package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
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
import us.huseli.thoucylinder.DownloadStatus
import us.huseli.thoucylinder.data.entities.Track
import us.huseli.thoucylinder.data.entities.YoutubeStreamDict
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
    private val playerRepo: PlayerRepository,
) : BaseDownloadViewModel() {
    private val _streamDict = MutableStateFlow<YoutubeStreamDict?>(null)
    private val _track = MutableStateFlow<Track?>(null)
    private val _uri = MutableStateFlow<Uri?>(null)
    private val _video = MutableStateFlow<YoutubeVideo?>(null)

    val isDownloaded: Flow<Boolean> = _track.map {
        it?.absolutePath?.isFile == true
    }
    val isPlaying: Flow<Boolean> =
        combine(playerRepo.currentUri.filterNotNull(), playerRepo.isPlaying) { uri, isPlaying ->
            isPlaying && uri == _uri.value
        }
    val streamDict = _streamDict.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _video.filterNotNull().distinctUntilChanged().collect { video ->
                _streamDict.value = youtubeRepo.getBestStreamDict(video.id)
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
            combine(_streamDict, _track) { streamDict, track ->
                track?.absolutePath?.takeIf { it.isFile }?.toUri() ?: streamDict?.url?.toUri()
            }.distinctUntilChanged().collect { uri ->
                _uri.value = uri
            }
        }
    }

    fun download() = viewModelScope.launch(Dispatchers.IO) {
        _video.value?.let { video ->
            try {
                val track = youtubeRepo.downloadTrack(
                    video = video,
                    progressCallback = { _downloadProgress.value = it },
                    statusCallback = { _downloadStatus.value = it },
                )
                repo.insertTrack(track)
            } catch (e: Exception) {
                Log.e("downloadTrack", e.toString(), e)
            } finally {
                _downloadProgress.value = 0.0
                _downloadStatus.value = DownloadStatus()
            }
        }
    }

    fun play() = viewModelScope.launch {
        _uri.value?.let { uri ->
            playerRepo.playOrPause(uri)
        }
    }

    fun setVideo(value: YoutubeVideo) {
        _video.value = value
    }
}
