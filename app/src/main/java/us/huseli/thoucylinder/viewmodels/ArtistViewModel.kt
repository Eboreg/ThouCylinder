package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
) : AbstractAlbumListViewModel("ArtistViewModel", repos) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val _albumPojos = MutableStateFlow<List<AlbumPojo>>(emptyList())

    val artist: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!

    val albumPojos = _albumPojos.asStateFlow()
    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val trackPojos: Flow<PagingData<TrackPojo>> =
        repos.track.pageTrackPojosByArtist(artist).flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            repos.album.flowAlbumPojosByArtist(artist)
                .distinctUntilChanged()
                .collect { pojos -> _albumPojos.value = pojos }
        }
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
