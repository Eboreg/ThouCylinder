package us.huseli.thoucylinder.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
) : ViewModel() {
    private val _fetchedThumbnails = mutableListOf<String>()
    private val _isSearching = MutableStateFlow(false)
    private val _thumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    val albums = repo.albums
    val isSearching = _isSearching.asStateFlow()
    val playlists = youtubeRepo.playlistSearchResults
    val videos = youtubeRepo.videoSearchResults

    fun getThumbnail(playlist: YoutubePlaylist) = MutableStateFlow<ImageBitmap?>(null).apply {
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

    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _isSearching.value = true
        youtubeRepo.search(query)
        _isSearching.value = false
    }
}
