package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.pojos.TagPojo
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
open class LibraryViewModel @Inject constructor(
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
    private val _selectedAlbumTagPojos = MutableStateFlow<List<TagPojo>>(emptyList())
    private val _selectedTrackTagPojos = MutableStateFlow<List<TagPojo>>(emptyList())
    private val _availabilityFilter = MutableStateFlow(AvailabilityFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val albumCombos = combine(
        _albumSortParameter,
        _albumSortOrder,
        _albumSearchTerm,
        _selectedAlbumTagPojos,
        _availabilityFilter,
    ) { sortParameter, sortOrder, searchTerm, tagPojos, availability ->
        repos.album.flowAlbumCombos(sortParameter, sortOrder, searchTerm, tagPojos.map { it.name }, availability)
    }.flattenMerge().onEach { _isLoadingAlbums.value = false }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingTrackCombos: Flow<PagingData<TrackCombo>> =
        combine(
            _trackSortParameter,
            _trackSortOrder,
            _trackSearchTerm,
            _selectedTrackTagPojos,
            _availabilityFilter,
        ) { sortParameter, sortOrder, searchTerm, tagPojos, availability ->
            repos.track.flowTrackComboPager(
                sortParameter = sortParameter,
                sortOrder = sortOrder,
                searchTerm = searchTerm,
                tagNames = tagPojos.map { it.name },
                availabilityFilter = availability,
            ).flow.cachedIn(viewModelScope)
        }.flattenMerge().onEach { _isLoadingTracks.value = false }.distinctUntilChanged()

    val albumSearchTerm = _albumSearchTerm.asStateFlow()
    val albumSortOrder = _albumSortOrder.asStateFlow()
    val albumSortParameter = _albumSortParameter.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val albumTagPojos = _availabilityFilter.flatMapMerge { repos.album.flowTagPojos(it) }
    val availabilityFilter = _availabilityFilter.asStateFlow()
    val displayType = _displayType.asStateFlow()
    val isImportingLocalMedia = repos.localMedia.isImportingLocalMedia
    val isLoadingAlbums = _isLoadingAlbums.asStateFlow()
    val isLoadingPlaylists = _isLoadingPlaylists.asStateFlow()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    val listType = _listType.asStateFlow()
    val playlists = repos.playlist.playlistsPojos
        .onStart { _isLoadingPlaylists.value = true }
        .onEach { _isLoadingPlaylists.value = false }
    val selectedAlbumTagPojos = _selectedAlbumTagPojos.asStateFlow()
    val selectedTrackTagPojos = _selectedTrackTagPojos.asStateFlow()
    val trackSearchTerm = _trackSearchTerm.asStateFlow()
    val trackSortOrder = _trackSortOrder.asStateFlow()
    val trackSortParameter = _trackSortParameter.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val trackTagPojos = _availabilityFilter.flatMapMerge { repos.track.flowTagPojos(it) }

    suspend fun getPlaylistImage(playlistId: UUID, context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        repos.playlist.listPlaylistAlbums(playlistId).firstNotNullOfOrNull { album ->
            album.albumArt?.getThumbnailImageBitmap(context)
        }
    }

    fun setAlbumSearchTerm(value: String) {
        _albumSearchTerm.value = value
    }

    fun setAlbumSorting(sortParameter: AlbumSortParameter, sortOrder: SortOrder) {
        _albumSortParameter.value = sortParameter
        _albumSortOrder.value = sortOrder
    }

    fun setAvailabilityFilter(value: AvailabilityFilter) {
        _availabilityFilter.value = value
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }

    fun setSelectedAlbumTagPojos(value: List<TagPojo>) {
        _selectedAlbumTagPojos.value = value
    }

    fun setSelectedTrackTagPojos(value: List<TagPojo>) {
        _selectedTrackTagPojos.value = value
    }

    fun setTrackSearchTerm(value: String) {
        _trackSearchTerm.value = value
    }

    fun setTrackSorting(sortParameter: TrackSortParameter, sortOrder: SortOrder) {
        _trackSortParameter.value = sortParameter
        _trackSortOrder.value = sortOrder
    }
}
