package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: LocalRepository,
    private val youtubeRepo: YoutubeRepository,
) : ViewModel() {
    private val _playlists = MutableStateFlow<List<YoutubePlaylist>>(emptyList())
    private val _videos = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val playlists = _playlists.asStateFlow()
    val videos = _videos.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val albums = repo.albums
    val singleTracks = repo.singleTracks

    fun search(query: String) = viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true
        val (playlists, videos) = youtubeRepo.search(query)
        _playlists.value = playlists
        _videos.value = videos
        _isLoading.value = false
    }
}
