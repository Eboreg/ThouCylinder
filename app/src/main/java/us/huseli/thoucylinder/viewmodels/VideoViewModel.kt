package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.data.entities.YoutubeStreamDict
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor() : BaseDownloadViewModel() {
    private var exoPlayer: ExoPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    private val _streamDict = MutableStateFlow<YoutubeStreamDict?>(null)

    val isPlaying = _isPlaying.asStateFlow()
    val streamDict = _streamDict.asStateFlow()

    fun download(video: YoutubeVideo, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            YoutubeRepository.downloadTrack(
                video = video,
                context = context,
                progressCallback = { _downloadProgress.value = it },
                statusCallback = { _downloadStatus.value = it },
            )
        } catch (e: Exception) {
            Log.e("downloadTrack", e.toString(), e)
        } finally {
            _downloadProgress.value = 0.0
            _downloadStatus.value = YoutubeRepository.DownloadStatus()
        }
    }

    fun loadStreamDict(video: YoutubeVideo) = viewModelScope.launch(Dispatchers.IO) {
        _streamDict.value = video.getBestStreamDict()
    }

    fun play(video: YoutubeVideo, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        video.getBestStreamDict()?.url?.let { url ->
            launch(Dispatchers.Main) {
                if (!_isPlaying.value) {
                    exoPlayer?.release()
                    exoPlayer = ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(url))
                        prepare()
                        play()
                    }
                    _isPlaying.value = true
                } else {
                    exoPlayer?.let {
                        _isPlaying.value = it.isPlaying
                        if (it.isPlaying) {
                            it.pause()
                            _isPlaying.value = false
                        } else {
                            it.play()
                            _isPlaying.value = true
                        }
                    }
                }
            }
        }
    }
}
