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
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
) : AbstractAlbumListViewModel("ArtistViewModel", repos) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val _albumCombos = MutableStateFlow<List<AlbumCombo>>(emptyList())

    val artist: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!

    val albumCombos = _albumCombos.asStateFlow()
    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val trackCombos: Flow<PagingData<TrackCombo>> =
        repos.track.pageTrackCombosByArtist(artist).flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            repos.album.flowAlbumCombosByArtist(artist)
                .distinctUntilChanged()
                .collect { combos -> _albumCombos.value = combos }
        }
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
