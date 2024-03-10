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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.AvailabilityFilter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenAlbumTab(
    viewModel: LibraryViewModel,
    appCallbacks: AppCallbacks,
    albumCombos: List<AlbumCombo>,
    isImporting: Boolean,
    displayType: DisplayType,
) {
    val context = LocalContext.current
    val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
    val searchTerm by viewModel.albumSearchTerm.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle(emptyList())
    val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()
    val tagPojos by viewModel.albumTagPojos.collectAsStateWithLifecycle(emptyList())
    val selectedTagPojos by viewModel.selectedAlbumTagPojos.collectAsStateWithLifecycle()
    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()

    val albumCallbacks = { combo: AlbumCombo ->
        AlbumCallbacks(
            combo = combo,
            appCallbacks = appCallbacks,
            context = context,
            onPlayClick = if (combo.album.isPlayable) {
                { viewModel.playAlbum(combo.album.albumId) }
            } else null,
            onEnqueueClick = if (combo.album.isPlayable) {
                { viewModel.enqueueAlbum(combo.album.albumId, context) }
            } else null,
            onAlbumLongClick = {
                viewModel.selectAlbumsFromLastSelected(combo.album.albumId, albumCombos.map { it.album.albumId })
            },
            onAlbumClick = {
                if (selectedAlbumIds.isNotEmpty()) viewModel.toggleAlbumSelected(combo.album.albumId)
                else appCallbacks.onAlbumClick(combo.album.albumId)
            },
        )
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

    Column(modifier = Modifier.fillMaxSize()) {
        ListActions(
            initialSearchTerm = searchTerm,
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            sortParameters = AlbumSortParameter.withLabels(context),
            sortDialogTitle = stringResource(R.string.album_order),
            onSort = { param, order -> viewModel.setAlbumSorting(param, order) },
            onSearch = { viewModel.setAlbumSearchTerm(it) },
            tagPojos = tagPojos,
            selectedTagPojos = selectedTagPojos,
            onTagsChange = { viewModel.setSelectedAlbumTagPojos(it) },
            availabilityFilter = availabilityFilter,
            onAvailabilityFilterChange = { viewModel.setAvailabilityFilter(it) },
            filterButtonSelected = selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL,
        )

        when (displayType) {
            DisplayType.LIST -> AlbumList(
                combos = albumCombos,
                albumCallbacks = albumCallbacks,
                albumSelectionCallbacks = albumSelectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                onEmpty = onEmpty,
                albumDownloadTasks = albumDownloadTasks,
                progressIndicatorStringRes = progressIndicatorStringRes,
                getThumbnail = { viewModel.getAlbumThumbnail(it, context) },
            )
            DisplayType.GRID -> AlbumGrid(
                combos = albumCombos,
                albumCallbacks = albumCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                albumSelectionCallbacks = albumSelectionCallbacks,
                onEmpty = onEmpty,
                albumDownloadTasks = albumDownloadTasks,
                progressIndicatorStringRes = progressIndicatorStringRes,
            )
        }
    }
}
