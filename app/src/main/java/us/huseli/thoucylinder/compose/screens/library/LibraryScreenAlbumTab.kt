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
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.enums.AlbumSortParameter
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenAlbumTab(
    appCallbacks: AppCallbacks,
    viewStates: ImmutableList<Album.ViewState>,
    isImporting: Boolean,
    displayType: DisplayType,
    showToolbars: Boolean,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    listModifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val downloadStates by viewModel.albumDownloadStates.collectAsStateWithLifecycle()
    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
    val searchTerm by viewModel.albumSearchTerm.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle(persistentListOf())
    val selectedTagPojos by viewModel.selectedAlbumTagPojos.collectAsStateWithLifecycle()
    val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()
    val tagPojos by viewModel.albumTagPojos.collectAsStateWithLifecycle(persistentListOf())

    val albumCallbacks = remember {
        { state: Album.ViewState ->
            AlbumCallbacks(
                state = state,
                appCallbacks = appCallbacks,
                onPlayClick = if (state.album.isPlayable) {
                    { viewModel.playAlbum(state.album.albumId) }
                } else null,
                onEnqueueClick = if (state.album.isPlayable) {
                    { viewModel.enqueueAlbum(state.album.albumId, context) }
                } else null,
                onAlbumLongClick = {
                    viewModel.selectAlbumsFromLastSelected(state.album.albumId, viewStates.map { it.album.albumId })
                },
                onAlbumClick = {
                    if (selectedAlbumIds.isNotEmpty()) viewModel.toggleAlbumSelected(state.album.albumId)
                    else appCallbacks.onAlbumClick(state.album.albumId)
                },
            )
        }
    }
    val albumSelectionCallbacks = remember { viewModel.getAlbumSelectionCallbacks(appCallbacks, context) }
    val progressIndicatorStringRes = remember(isImporting, isLoadingAlbums) {
        if (isImporting) R.string.importing_local_albums
        else if (isLoadingAlbums) R.string.loading_albums
        else null
    }
    val onEmpty: @Composable () -> Unit = {
        if (!isImporting && !isLoadingAlbums) {
            Text(
                stringResource(R.string.no_albums_found),
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    CollapsibleToolbar(show = showToolbars) {
        ListSettingsRow(
            displayType = displayType,
            listType = ListType.ALBUMS,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = listOf(DisplayType.LIST, DisplayType.GRID),
        )
        ListActions(
            initialSearchTerm = searchTerm,
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            sortParameters = AlbumSortParameter.withLabels(context),
            sortDialogTitle = stringResource(R.string.album_order),
            onSort = { param, order -> viewModel.setAlbumSorting(param, order) },
            onSearch = { viewModel.setAlbumSearchTerm(it) },
            filterButtonSelected = selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL,
            tagPojos = tagPojos,
            selectedTagPojos = selectedTagPojos,
            availabilityFilter = availabilityFilter,
            onTagsChange = { viewModel.setSelectedAlbumTagPojos(it) },
            onAvailabilityFilterChange = { viewModel.setAvailabilityFilter(it) },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (displayType) {
            DisplayType.LIST -> AlbumList(
                states = viewStates,
                callbacks = albumCallbacks,
                selectionCallbacks = albumSelectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                onEmpty = onEmpty,
                progressIndicatorStringRes = progressIndicatorStringRes,
                modifier = listModifier,
                downloadStates = downloadStates,
            )
            DisplayType.GRID -> AlbumGrid(
                states = viewStates,
                callbacks = albumCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                selectionCallbacks = albumSelectionCallbacks,
                onEmpty = onEmpty,
                progressIndicatorStringRes = progressIndicatorStringRes,
                modifier = listModifier,
                downloadStates = downloadStates,
            )
        }
    }
}
