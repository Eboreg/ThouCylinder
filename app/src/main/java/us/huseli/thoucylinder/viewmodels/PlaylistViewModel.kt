package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubePlaylistVideo
import us.huseli.thoucylinder.repositories.YoutubeRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor() : BaseDownloadViewModel() {
    private val _thumbnail = MutableStateFlow<ImageBitmap?>(null)
    private val _thumbnailLoadStatus = MutableStateFlow(LoadStatus.NOT_LOADED)
    private val _videos = MutableStateFlow<List<YoutubePlaylistVideo>>(emptyList())
    private val _videosLoadStatus = MutableStateFlow(LoadStatus.NOT_LOADED)

    val thumbnail = _thumbnail.asStateFlow()
    val thumbnailLoadStatus = _thumbnailLoadStatus.asStateFlow()
    val videos = _videos.asStateFlow()
    val videosLoadStatus = _videosLoadStatus.asStateFlow()

    fun download(playlist: YoutubePlaylist, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            YoutubeRepository.downloadPlaylist(
                playlist = playlist,
                context = context,
                progressCallback = { _downloadProgress.value = it },
                statusCallback = { _downloadStatus.value = it },
            )
        } catch (e: Exception) {
            Log.e("downloadPlaylist", e.toString(), e)
        } finally {
            _downloadStatus.value = YoutubeRepository.DownloadStatus()
            _downloadProgress.value = 0.0
        }
    }

    fun loadThumbnail(playlist: YoutubePlaylist, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        if (_thumbnailLoadStatus.value == LoadStatus.NOT_LOADED) {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }

            _thumbnailLoadStatus.value = LoadStatus.LOADING
            playlist.thumbnail?.url?.let { url ->
                YoutubeRepository.getPlaylistThumbnail(playlist.id, url, dir)?.let { bitmap ->
                    _thumbnail.value = bitmap
                }
            }
            _thumbnailLoadStatus.value = LoadStatus.LOADED
        }
    }

    fun loadVideos(playlist: YoutubePlaylist) = viewModelScope.launch(Dispatchers.IO) {
        if (_videosLoadStatus.value == LoadStatus.NOT_LOADED) {
            _videosLoadStatus.value = LoadStatus.LOADING
            _videos.value = YoutubeRepository.listPlaylistVideos(playlist)
            _videosLoadStatus.value = LoadStatus.LOADED
        }
    }
}
