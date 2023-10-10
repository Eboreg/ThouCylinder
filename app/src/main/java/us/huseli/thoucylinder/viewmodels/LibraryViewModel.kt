package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.ArtistPojo
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repos: Repositories) : BaseViewModel(repos) {
    private val _artistImages = MutableStateFlow<Map<String, Image>>(emptyMap())
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)

    val albumPojos = repos.local.albumPojos
    val artistImages = _artistImages.asStateFlow()
    val artistPojos: Flow<List<ArtistPojo>> = repos.local.artistPojos
    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val pagingTracks: Flow<PagingData<Track>> = repos.local.trackPager.flow.cachedIn(viewModelScope)
    val playlists = repos.local.playlists

    init {
        viewModelScope.launch(Dispatchers.IO) { _artistImages.value = repos.mediaStore.collectArtistImages() }
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) { repos.local.deleteAll() }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
