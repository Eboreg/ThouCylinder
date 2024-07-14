package us.huseli.thoucylinder.compose.library

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.album.AlbumCollection
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.compose.utils.EmptyLibraryHelp
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenAlbumTab(
    uiStates: () -> ImmutableList<AlbumUiState>,
    modifier: Modifier = Modifier,
    displayType: DisplayType = DisplayType.LIST,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val callbacks = LocalAlbumCallbacks.current
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val isAlbumsEmpty by viewModel.isAlbumsEmpty.collectAsStateWithLifecycle()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
    val selectedAlbumCount = viewModel.selectedAlbumCount.collectAsStateWithLifecycle().asIntState()

    AlbumCollection(
        displayType = displayType,
        onClick = remember { { viewModel.onAlbumClick(it, callbacks.onGotoAlbumClick) } },
        onLongClick = remember { { viewModel.onAlbumLongClick(it) } },
        selectedAlbumCount = { selectedAlbumCount.intValue },
        selectionCallbacks = viewModel.getAlbumSelectionCallbacks(dialogCallbacks),
        states = uiStates,
        downloadStateFlow = { viewModel.getAlbumDownloadUiStateFlow(it) },
        modifier = modifier,
        isLoading = isLoadingAlbums,
        scrollbarListState = rememberScrollbarListState(),
        scrollbarGridState = rememberScrollbarGridState(),
        onEmpty = {
            if (isAlbumsEmpty) {
                Text(stringResource(R.string.no_albums_found))
                EmptyLibraryHelp()
            }
        }
    )
}

@Composable
fun AlbumListActions(viewModel: LibraryViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()
    val searchTerm by viewModel.albumSearchTerm.collectAsStateWithLifecycle()
    val selectedTagPojos by viewModel.selectedAlbumTagPojos.collectAsStateWithLifecycle()
    val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()
    val tagPojos by viewModel.albumTagPojos.collectAsStateWithLifecycle()

    val filterButtonSelected = remember(selectedTagPojos, availabilityFilter) {
        selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL
    }

    ListActions(
        searchTerm = { searchTerm },
        sortParameter = sortParameter,
        sortOrder = sortOrder,
        sortParameters = remember { AlbumSortParameter.withLabels(context) },
        sortDialogTitle = stringResource(R.string.album_order),
        onSort = remember { { param, order -> viewModel.setAlbumSorting(param, order) } },
        onSearch = remember { { viewModel.setAlbumSearchTerm(it) } },
        filterButtonSelected = filterButtonSelected,
        tagPojos = { tagPojos },
        selectedTagPojos = { selectedTagPojos },
        availabilityFilter = availabilityFilter,
        onTagsChange = remember { { viewModel.setSelectedAlbumTagPojos(it) } },
        onAvailabilityFilterChange = remember { { viewModel.setAvailabilityFilter(it) } },
    )
}
