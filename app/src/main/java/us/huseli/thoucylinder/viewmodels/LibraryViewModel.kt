package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractAlbumListViewModel("LibraryViewModel", repos) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _isLoadingAlbums = MutableStateFlow(true)
    private val _isLoadingPlaylists = MutableStateFlow(true)
    private val _isLoadingTracks = MutableStateFlow(true)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val _albumSortParameter = MutableStateFlow(AlbumSortParameter.ARTIST)
    private val _albumSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _trackSortParameter = MutableStateFlow(TrackSortParameter.TITLE)
    private val _trackSortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _albumSearchTerm = MutableStateFlow("")
    private val _trackSearchTerm = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumCombos: Flow<List<AlbumCombo>> =
        combine(_albumSortParameter, _albumSortOrder, _albumSearchTerm) { sortParameter, sortOrder, searchTerm ->
            repos.album.flowAlbumCombos(sortParameter, sortOrder, searchTerm)
        }.flattenMerge().onStart { _isLoadingAlbums.value = true }.onEach { _isLoadingAlbums.value = false }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingTrackCombos: Flow<PagingData<TrackCombo>> =
        combine(_trackSortParameter, _trackSortOrder, _trackSearchTerm) { sortParameter, sortOrder, searchTerm ->
            repos.track.flowTrackComboPager(sortParameter, sortOrder, searchTerm).flow.cachedIn(viewModelScope)
        }.flattenMerge().onStart { _isLoadingTracks.value = true }.onEach { _isLoadingTracks.value = false }

    val displayType = _displayType.asStateFlow()
    val isImportingLocalMedia = repos.localMedia.isImportingLocalMedia
    val isLoadingAlbums = _isLoadingAlbums.asStateFlow()
    val isLoadingPlaylists = _isLoadingPlaylists.asStateFlow()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    val listType = _listType.asStateFlow()
    val playlists = repos.playlist.playlistsPojos
        .onStart { _isLoadingPlaylists.value = true }
        .onEach { _isLoadingPlaylists.value = false }
    val albumSortParameter = _albumSortParameter.asStateFlow()
    val albumSortOrder = _albumSortOrder.asStateFlow()
    val trackSortParameter = _trackSortParameter.asStateFlow()
    val trackSortOrder = _trackSortOrder.asStateFlow()
    val albumSearchTerm = _albumSearchTerm.asStateFlow()
    val trackSearchTerm = _trackSearchTerm.asStateFlow()

    suspend fun getPlaylistImage(playlistId: UUID, context: Context): ImageBitmap? =
        repos.playlist.listPlaylistAlbums(playlistId).firstNotNullOfOrNull { album ->
            album.albumArt?.getThumbnailImageBitmap(context)
        }

    fun setAlbumSearchTerm(value: String) {
        _albumSearchTerm.value = value
    }

    fun setAlbumSorting(sortParameter: AlbumSortParameter, sortOrder: SortOrder) {
        _albumSortParameter.value = sortParameter
        _albumSortOrder.value = sortOrder
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }

    fun setTrackSearchTerm(value: String) {
        _trackSearchTerm.value = value
    }

    fun setTrackSorting(sortParameter: TrackSortParameter, sortOrder: SortOrder) {
        _trackSortParameter.value = sortParameter
        _trackSortOrder.value = sortOrder
    }
}
