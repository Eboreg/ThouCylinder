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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractAlbumListViewModel("LibraryViewModel", repos) {
    private val _artistImages = MutableStateFlow<Map<String, File>>(emptyMap())
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _isLoadingAlbums = MutableStateFlow(true)
    private val _isLoadingArtists = MutableStateFlow(true)
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
    val albumPojos: Flow<List<AlbumPojo>> =
        combine(_albumSortParameter, _albumSortOrder, _albumSearchTerm) { sortParameter, sortOrder, searchTerm ->
            repos.room.flowAlbumPojos(sortParameter, sortOrder, searchTerm)
        }.flattenMerge().onCompletion { _isLoadingAlbums.value = false }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingTrackPojos: Flow<PagingData<TrackPojo>> =
        combine(_trackSortParameter, _trackSortOrder, _trackSearchTerm) { sortParameter, sortOrder, searchTerm ->
            repos.room.flowTrackPojoPager(sortParameter, sortOrder, searchTerm).flow.cachedIn(viewModelScope)
        }.flattenMerge().onCompletion { _isLoadingTracks.value = false }

    val artistImages = _artistImages.asStateFlow()
    val artistPojos = repos.room.artistPojos.onCompletion { _isLoadingArtists.value = false }
    val displayType = _displayType.asStateFlow()
    val isImportingLocalMedia = repos.mediaStore.isImportingLocalMedia
    val isLoadingAlbums = _isLoadingAlbums.asStateFlow()
    val isLoadingArtists = _isLoadingArtists.asStateFlow()
    val isLoadingPlaylists = _isLoadingPlaylists.asStateFlow()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    val listType = _listType.asStateFlow()
    val playlists = repos.room.playlists.onCompletion { _isLoadingPlaylists.value = false }
    val albumSortParameter = _albumSortParameter.asStateFlow()
    val albumSortOrder = _albumSortOrder.asStateFlow()
    val trackSortParameter = _trackSortParameter.asStateFlow()
    val trackSortOrder = _trackSortOrder.asStateFlow()
    val albumSearchTerm = _albumSearchTerm.asStateFlow()
    val trackSearchTerm = _trackSearchTerm.asStateFlow()

    init {
        viewModelScope.launch { _artistImages.value = repos.mediaStore.collectArtistImages() }
    }

    suspend fun getPlaylistImage(playlistId: UUID, context: Context): ImageBitmap? =
        repos.room.listPlaylistAlbums(playlistId).firstNotNullOfOrNull { album ->
            album.getThumbnail(context)
        }

    fun selectAlbumsFromLastSelected(to: Album) = viewModelScope.launch {
        selectAlbumsFromLastSelected(albums = albumPojos.first().map { it.album }, to = to)
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
