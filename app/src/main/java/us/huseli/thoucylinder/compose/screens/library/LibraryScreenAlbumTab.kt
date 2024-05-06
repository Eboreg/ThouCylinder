package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.getUmlautifiedString
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenAlbumTab(
    appCallbacks: AppCallbacks,
    albumCallbacks: AlbumCallbacks,
    uiStates: () -> ImmutableList<AlbumUiState>,
    modifier: Modifier = Modifier,
    isImporting: Boolean = false,
    displayType: DisplayType = DisplayType.LIST,
    showToolbars: () -> Boolean = { true },
    viewModel: LibraryViewModel = hiltViewModel(),
    listModifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
    val searchTerm by viewModel.albumSearchTerm.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle()
    val selectedTagPojos by viewModel.selectedAlbumTagPojos.collectAsStateWithLifecycle()
    val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()
    val tagPojos by viewModel.albumTagPojos.collectAsStateWithLifecycle()

    val albumSelectionCallbacks = remember { viewModel.getAlbumSelectionCallbacks(appCallbacks, context) }
    val filterButtonSelected = remember(selectedTagPojos, availabilityFilter) {
        selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL
    }
    val progressIndicatorText = remember(isImporting, isLoadingAlbums) {
        {
            if (isImporting) context.getUmlautifiedString(R.string.importing_local_albums)
            else if (isLoadingAlbums) context.getUmlautifiedString(R.string.loading_albums)
            else null
        }
    }
    val onEmpty: @Composable () -> Unit = remember {
        {
            if (!isImporting && !isLoadingAlbums) {
                Text(
                    stringResource(R.string.no_albums_found),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }

    CollapsibleToolbar(show = showToolbars) {
        ListSettingsRow(
            displayType = displayType,
            listType = ListType.ALBUMS,
            onDisplayTypeChange = remember { { viewModel.setDisplayType(it) } },
            onListTypeChange = remember { { viewModel.setListType(it) } },
            availableDisplayTypes = persistentListOf(DisplayType.LIST, DisplayType.GRID),
        )
        ListActions(
            initialSearchTerm = searchTerm,
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

    Column(modifier = modifier.fillMaxSize()) {
        when (displayType) {
            DisplayType.LIST -> AlbumList(
                states = uiStates,
                selectionCallbacks = albumSelectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                onEmpty = onEmpty,
                progressIndicatorText = progressIndicatorText,
                modifier = listModifier,
                callbacks = remember {
                    albumCallbacks.copy(
                        onAlbumClick = { viewModel.onAlbumClick(it, albumCallbacks.onAlbumClick) },
                        onAlbumLongClick = { viewModel.onAlbumLongClick(it) },
                    )
                },
            )
            DisplayType.GRID -> AlbumGrid(
                states = uiStates,
                callbacks = remember {
                    albumCallbacks.copy(
                        onAlbumClick = { viewModel.onAlbumClick(it, albumCallbacks.onAlbumClick) },
                        onAlbumLongClick = { viewModel.onAlbumLongClick(it) },
                    )
                },
                selectionCallbacks = albumSelectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                modifier = listModifier,
                progressIndicatorText = progressIndicatorText,
                onEmpty = onEmpty,
            )
        }
    }
}
