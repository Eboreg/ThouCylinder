package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.getAlbumUiStateFlow
import us.huseli.thoucylinder.getTrackUiStateFlow
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
    private val managers: Managers,
) : AbstractAlbumListViewModel("ArtistViewModel", repos, managers) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val artistId: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!

    override val albumUiStates: StateFlow<ImmutableList<AlbumUiState>> = combine(
        repos.album.flowAlbumCombosByArtist(artistId),
        managers.library.albumDownloadTasks,
    ) { combos, tasks ->
        combos.map { combo ->
            AlbumUiState.fromAlbumCombo(combo).copy(downloadState = tasks.getAlbumUiStateFlow(combo.album.albumId))
        }.toImmutableList()
    }.stateLazily(persistentListOf())

    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()

    val trackUiStates: Flow<PagingData<TrackUiState>> = combine(
        repos.track.pageTrackCombosByArtist(artistId).flow,
        managers.library.trackDownloadTasks,
    ) { pagingData, tasks ->
        pagingData.map { combo ->
            TrackUiState.fromTrackCombo(combo).copy(downloadState = tasks.getTrackUiStateFlow(combo.track.trackId))
        }
    }

    val artist: StateFlow<Artist?> = repos.artist.flowArtistById(artistId).distinctUntilChanged().stateLazily()

    fun enqueueArtist() = managers.player.enqueueArtist(artistId)

    fun ensureTrackMetadata(uiState: TrackUiState) = managers.library.ensureTrackMetadataAsync(uiState.trackId)

    fun ensureTrackMetadata(track: Track) = managers.library.ensureTrackMetadataAsync(track)

    fun onAllArtistTrackIds(callback: (ImmutableList<String>) -> Unit) {
        launchOnIOThread { callback(repos.track.listTrackIdsByArtistId(artistId).toImmutableList()) }
    }

    fun playArtist() = managers.player.playArtist(artistId)

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }
}
