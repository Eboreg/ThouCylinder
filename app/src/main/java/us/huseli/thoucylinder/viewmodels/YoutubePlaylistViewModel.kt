package us.huseli.thoucylinder.viewmodels

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_PLAYLIST
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.TempAlbum
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubePlaylistVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class YoutubePlaylistViewModel @Inject constructor(
    private val repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val playlistId: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _albumArtLoadStatus = MutableStateFlow(LoadStatus.LOADING)
    private val _downloadedAlbum = MutableStateFlow<TempAlbum?>(null)
    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    private val _playlist = MutableStateFlow<YoutubePlaylist?>(null)
    private val _videos = MutableStateFlow<List<YoutubePlaylistVideo>>(emptyList())
    private val _videosLoadStatus = MutableStateFlow(LoadStatus.LOADING)

    val albumArt = _albumArt.asStateFlow()
    val albumArtLoadStatus = _albumArtLoadStatus.asStateFlow()
    val downloadedAlbum = _downloadedAlbum.asStateFlow()
    val downloadProgress = _downloadProgress.asStateFlow()
    val isDownloaded: Flow<Boolean> =
        combine(_playlist.filterNotNull(), repo.albums, _videos) { playlist, albums, videos ->
            val album = albums.find { it.youtubePlaylist?.id == playlist.id }
            album != null &&
                album.tracks.size >= playlist.videoCount &&
                videos.all { album.tracks.map { track -> track.youtubeVideo?.id }.contains(it.video.id) }
        }.distinctUntilChanged()
    val playlist = _playlist.asStateFlow()
    val videos = _videos.asStateFlow()
    val videosLoadStatus = _videosLoadStatus.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _playlist.value = youtubeRepo.getPlaylist(playlistId)?.also {
                _videos.value = youtubeRepo.listPlaylistVideos(it)
                _videosLoadStatus.value = LoadStatus.LOADED

                _albumArt.value = it.thumbnail?.getImageBitmap()
                _albumArtLoadStatus.value = LoadStatus.LOADED
            }
        }
    }

    fun deleteDownloadedAlbum() {
        _downloadedAlbum.value?.let { album ->
            viewModelScope.launch(Dispatchers.IO) {
                album.tracks.forEach { track ->
                    track.localFile.delete()
                }
            }
        }
        _downloadedAlbum.value = null
    }

    fun download() = viewModelScope.launch(Dispatchers.IO) {
        _playlist.value?.let { playlist ->
            if (_videos.value.isEmpty()) _videos.value = youtubeRepo.listPlaylistVideos(playlist)
            try {
                val album = youtubeRepo.downloadPlaylist(
                    playlist = playlist,
                    videos = _videos.value,
                ) { _downloadProgress.value = it }
                _downloadedAlbum.value = album
            } catch (e: Exception) {
                Log.e("downloadPlaylist", e.toString(), e)
            } finally {
                _downloadProgress.value = null
            }
        }
    }

    fun saveDownloadedAlbum(album: TempAlbum) = viewModelScope.launch {
        _downloadedAlbum.value = null
        repo.moveAndInsertAlbum(album) { _downloadProgress.value = it }
        _downloadProgress.value = null
    }
}
