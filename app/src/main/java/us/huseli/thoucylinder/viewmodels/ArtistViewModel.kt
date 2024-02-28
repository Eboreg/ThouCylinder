package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.launchOnIOThread
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
) : AbstractAlbumListViewModel("ArtistViewModel", repos) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val artistId: UUID = UUID.fromString(savedStateHandle.get<String>(NAV_ARG_ARTIST)!!)

    override val albumCombos = repos.album.flowAlbumCombosByArtist(artistId)

    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val trackCombos: Flow<PagingData<TrackCombo>> =
        repos.track.pageTrackCombosByArtist(artistId).flow.cachedIn(viewModelScope)
    val artist: Flow<Artist?> = repos.artist.flowArtistById(artistId)

    init {
        launchOnIOThread {
            artist.filterNotNull().collect {
                if (it.musicBrainzId == null) repos.musicBrainz.matchArtist(it)?.also { updatedArtist ->
                    repos.artist.updateArtist(updatedArtist)
                }
            }
        }
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
