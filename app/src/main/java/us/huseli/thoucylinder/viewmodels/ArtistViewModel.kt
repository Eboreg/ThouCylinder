package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    repos: Repositories,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel(repos) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)

    val artist: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!

    val albumPojos = repos.local.albumPojos.map { pojos -> pojos.filter { pojo -> pojo.album.artist == artist } }
    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val tracks = repos.local.pageTracksByArtist(artist).flow.cachedIn(viewModelScope)

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
