package us.huseli.thoucylinder.viewmodels

import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import us.huseli.thoucylinder.DownloadStatus
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.data.entities.Album
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubePlaylistVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
) : BaseDownloadViewModel() {
    private val _albumArt = MutableStateFlow<ImageBitmap?>(null)
    private val _albumArtLoadStatus = MutableStateFlow(LoadStatus.NOT_LOADED)
    private val _playlist = MutableStateFlow<YoutubePlaylist?>(null)
    private val _showVideos = MutableStateFlow(false)
    private val _videos = MutableStateFlow<List<YoutubePlaylistVideo>>(emptyList())
    private val _videosLoadStatus = MutableStateFlow(LoadStatus.NOT_LOADED)

    private val _album: Flow<Album> = combine(_playlist, repo.albums) { playlist, albums ->
        playlist?.let { albums.find { album -> album.youtubePlaylist?.id == playlist.id } }
    }.filterNotNull().distinctUntilChanged()

    val albumArt = _albumArt.asStateFlow()
    val albumArtLoadStatus = _albumArtLoadStatus.asStateFlow()
    val isDownloaded: Flow<Boolean> = combine(_playlist.filterNotNull(), _album, _videos) { playlist, album, videos ->
        album.tracks.size >= playlist.videoCount &&
            videos.all { album.tracks.map { track -> track.youtubeVideo?.id }.contains(it.video.id) }
    }.distinctUntilChanged()
    val showVideos = _showVideos.asStateFlow()
    val videos = _videos.asStateFlow()
    val videosLoadStatus = _videosLoadStatus.asStateFlow()

    fun download(playlist: YoutubePlaylist) = viewModelScope.launch(Dispatchers.IO) {
        if (_videos.value.isEmpty()) _videos.value = youtubeRepo.listPlaylistVideos(playlist)
        try {
            val album = youtubeRepo.downloadPlaylist(
                playlist = playlist,
                videos = _videos.value,
                progressCallback = { _downloadProgress.value = it },
                statusCallback = { _downloadStatus.value = it },
            )
            repo.insertAlbumWithTracks(album)
        } catch (e: Exception) {
            Log.e("downloadPlaylist", e.toString(), e)
        } finally {
            _downloadStatus.value = DownloadStatus()
            _downloadProgress.value = 0.0
        }
    }

    fun setPlaylist(value: YoutubePlaylist) {
        _playlist.value = value
    }

    fun toggleShowVideos() {
        _showVideos.value = !_showVideos.value

        if (_showVideos.value) {
            if (_albumArtLoadStatus.value == LoadStatus.NOT_LOADED) loadAlbumArt()
            if (_videosLoadStatus.value == LoadStatus.NOT_LOADED) loadVideos()
        }
    }

    private fun loadAlbumArt() = viewModelScope.launch(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            _albumArtLoadStatus.value = LoadStatus.LOADING
            _playlist.value?.thumbnail?.getFile()?.let { file ->
                _albumArt.value = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)).asImageBitmap()
            }
            _albumArtLoadStatus.value = LoadStatus.LOADED
        } else _albumArtLoadStatus.value = LoadStatus.ERROR
    }

    private fun loadVideos() = viewModelScope.launch(Dispatchers.IO) {
        _playlist.value?.let { playlist ->
            _videosLoadStatus.value = LoadStatus.LOADING
            _videos.value = youtubeRepo.listPlaylistVideos(playlist)
            _videosLoadStatus.value = LoadStatus.LOADED
        }
    }
}
