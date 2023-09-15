package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private var exoPlayer: ExoPlayer? = null
    private val _playlists = MutableStateFlow<List<YoutubePlaylist>>(emptyList())
    private val _videos = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    private val _isPlaying = MutableStateFlow(false)
    private val _playingVideo = MutableStateFlow<YoutubeVideo?>(null)
    private val _playlistThumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    val playlists = _playlists.asStateFlow()
    val videos = _videos.asStateFlow()
    val isPlaying = _isPlaying.asStateFlow()
    val playingVideo = _playingVideo.asStateFlow()
    val playlistThumbnails = _playlistThumbnails.asStateFlow()

    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        val (playlists, videos) = YoutubeRepository.search(query)
        _playlists.value = playlists
        _videos.value = videos
    }

    fun downloadTrack(video: YoutubeVideo, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            YoutubeRepository.downloadTrack(video, context.filesDir)
        } catch (e: Exception) {
            Log.e("download", e.toString(), e)
        }
    }

    fun play(video: YoutubeVideo, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            video.streamUrl?.let { url ->
                launch(Dispatchers.Main) {
                    if (_playingVideo.value != video) {
                        exoPlayer?.release()
                        exoPlayer = ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(url))
                            prepare()
                            play()
                        }
                        _isPlaying.value = true
                        _playingVideo.value = video
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

    fun loadPlaylistThumbnail(playlist: YoutubePlaylist, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        if (!_playlistThumbnails.value.containsKey(playlist.id)) {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            YoutubeRepository.getPlaylistThumbnail(playlist, dir)?.let { bitmap ->
                _playlistThumbnails.value += Pair(playlist.id, bitmap)
            }
        }
    }
}
