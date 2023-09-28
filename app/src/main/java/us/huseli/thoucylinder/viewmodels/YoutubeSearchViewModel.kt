package us.huseli.thoucylinder.viewmodels

import androidx.annotation.MainThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val youtubeRepo: YoutubeRepository,
    private val repo: LocalRepository,
) : ViewModel() {
    private val _fetchedThumbnails = mutableListOf<String>()
    private val _isSearching = MutableStateFlow(false)
    private val _thumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    val isSearching = _isSearching.asStateFlow()
    val playlists = youtubeRepo.playlistSearchResults
    val videos = youtubeRepo.videoSearchResults

    fun addYoutubePlaylistAsTempAlbum(playlist: YoutubePlaylist, @MainThread onFinish: (Album) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val videos = youtubeRepo.listPlaylistVideos(playlist = playlist, withMetadata = false)
            val album = repo.createTempAlbumFromYoutubePlaylist(playlist, videos)
            withContext(Dispatchers.Main) { onFinish(album) }
        }

    fun getPlaylistThumbnail(playlist: YoutubePlaylist) = MutableStateFlow<ImageBitmap?>(null).apply {
        val thumbnail = _thumbnails.value[playlist.id]

        if (thumbnail != null) value = thumbnail
        else viewModelScope.launch(Dispatchers.IO) {
            if (!_fetchedThumbnails.contains(playlist.id)) {
                _fetchedThumbnails.add(playlist.id)
                playlist.thumbnail?.getImageBitmap()?.also {
                    value = it
                    _thumbnails.value += playlist.id to it
                }
            }
        }
    }.asStateFlow()

    fun getVideoMetadata(video: YoutubeVideo) = MutableStateFlow<YoutubeMetadata?>(null).apply {
        if (video.metadata != null) value = video.metadata
        else viewModelScope.launch(Dispatchers.IO) {
            value = youtubeRepo.getBestMetadata(video.id)
        }
    }

    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _isSearching.value = true
        youtubeRepo.search(query)
        _isSearching.value = false
    }
}
