package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.tag.TagPojo
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.dataclasses.track.toUiStates
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.enums.SortOrder
import us.huseli.thoucylinder.enums.TrackSortParameter
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
open class LibraryViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractAlbumListViewModel<AlbumUiState>("LibraryViewModel", repos, managers) {
    private val _isLoadingAlbums = MutableStateFlow(true)
    private val _isLoadingTracks = MutableStateFlow(true)

    private val _trackCombos: Flow<List<TrackCombo>> = combine(
        repos.settings.trackSortParameter,
        repos.settings.trackSortOrder,
        repos.settings.trackSearchTerm,
        repos.settings.libraryTrackTagFilter,
        repos.settings.libraryAvailabilityFilter,
    ) { sortParameter, sortOrder, searchTerm, tagPojos, availability ->
        repos.track.flowTrackCombos(
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            searchTerm = searchTerm,
            tagNames = tagPojos.map { it.name },
            availabilityFilter = availability,
        )
    }.flattenMerge().onEach { _isLoadingTracks.value = false }.distinctUntilChanged()

    private val _albumUiStates = combine(
        repos.settings.albumSortParameter,
        repos.settings.albumSortOrder,
        repos.settings.albumSearchTerm,
        repos.settings.libraryAlbumTagFilter,
        repos.settings.libraryAvailabilityFilter,
    ) { sortParameter, sortOrder, searchTerm, tagPojos, availability ->
        repos.album.flowAlbumCombos(sortParameter, sortOrder, searchTerm, tagPojos.map { it.name }, availability)
            .map { combos -> combos.map { it.toUiState() }.toImmutableList() }
    }.flattenMerge().onEach { _isLoadingAlbums.value = false }

    override val baseAlbumUiStates: StateFlow<ImmutableList<AlbumUiState>> =
        _albumUiStates.stateLazily(persistentListOf())

    val albumSearchTerm = repos.settings.albumSearchTerm
    val albumSortOrder = repos.settings.albumSortOrder
    val albumSortParameter = repos.settings.albumSortParameter
    val albumTagPojos = repos.settings.libraryAvailabilityFilter.flatMapLatest { repos.album.flowTagPojos(it) }
        .map { it.toImmutableList() }
        .stateLazily(persistentListOf())
    val availabilityFilter = repos.settings.libraryAvailabilityFilter
    val displayType = repos.settings.libraryDisplayType
    val isAlbumsEmpty: StateFlow<Boolean> = combine(
        _albumUiStates,
        _isLoadingAlbums,
        repos.localMedia.isImportingLocalMedia,
    ) { states, isLoading, isImporting ->
        states.isEmpty() && !isLoading && !isImporting
    }.stateLazily(false)
    val isImportingLocalMedia = repos.localMedia.isImportingLocalMedia
    val isLoadingAlbums = _isLoadingAlbums.asStateFlow()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    val isLocalMediaDirConfigured: StateFlow<Boolean> =
        repos.settings.localMusicUri.map { it != null }.stateLazily(false)
    val isTracksEmpty: StateFlow<Boolean> = combine(
        _trackCombos,
        _isLoadingTracks,
        repos.localMedia.isImportingLocalMedia,
    ) { combos, isLoading, isImporting ->
        combos.isEmpty() && !isLoading && !isImporting
    }.stateLazily(false)
    val listType = repos.settings.libraryListType
    val selectedAlbumTagPojos = repos.settings.libraryAlbumTagFilter
    val selectedTrackTagPojos = repos.settings.libraryTrackTagFilter
    val trackSearchTerm = repos.settings.trackSearchTerm
    val trackSortOrder = repos.settings.trackSortOrder
    val trackSortParameter = repos.settings.trackSortParameter
    val trackTagPojos = repos.settings.libraryAvailabilityFilter.flatMapLatest { repos.track.flowTagPojos(it) }
        .map { it.toImmutableList() }
        .stateLazily(persistentListOf())

    override val baseTrackUiStates = _trackCombos.map { it.toUiStates() }.stateEagerly(persistentListOf())

    fun getAlbumDownloadUiStateFlow(albumId: String) =
        managers.library.getAlbumDownloadUiStateFlow(albumId).stateLazily()

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateLazily()

    fun importNewLocalAlbums() {
        launchOnIOThread { managers.library.importNewLocalAlbums() }
    }

    fun setAlbumSearchTerm(value: String) = repos.settings.setAlbumSearchTerm(value)

    fun setAlbumSorting(sortParameter: AlbumSortParameter, sortOrder: SortOrder) =
        repos.settings.setAlbumSorting(sortParameter, sortOrder)

    fun setAvailabilityFilter(value: AvailabilityFilter) = repos.settings.setLibraryAvailabilityFilter(value)

    fun setDisplayType(value: DisplayType) = repos.settings.setLibraryDisplayType(value)

    fun setListType(value: ListType) = repos.settings.setLibraryListType(value)

    fun setSelectedAlbumTagPojos(value: List<TagPojo>) = repos.settings.setLibraryAlbumTagFilter(value)

    fun setSelectedTrackTagPojos(value: List<TagPojo>) = repos.settings.setLibraryTrackTagFilter(value)

    fun setTrackSearchTerm(value: String) = repos.settings.setTrackSearchTerm(value)

    fun setTrackSorting(sortParameter: TrackSortParameter, sortOrder: SortOrder) =
        repos.settings.setTrackSorting(sortParameter, sortOrder)
}
