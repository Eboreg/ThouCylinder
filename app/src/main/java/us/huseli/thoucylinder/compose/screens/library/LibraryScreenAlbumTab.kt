package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
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
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
    val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()

    val albumCallbacks = { combo: AlbumCombo ->
        AlbumCallbacks(
            combo = combo,
            appCallbacks = appCallbacks,
            context = context,
            onPlayClick = { viewModel.playAlbum(combo.album) },
            onEnqueueClick = { viewModel.enqueueAlbum(combo.album, context) },
            onAlbumLongClick = {
                viewModel.selectAlbumsFromLastSelected(combo.album, albumCombos.map { it.album })
            },
            onAlbumClick = {
                if (selectedAlbums.isNotEmpty()) viewModel.toggleSelected(combo.album)
                else appCallbacks.onAlbumClick(combo.album.albumId)
            },
        )
    }
    val albumSelectionCallbacks = AlbumSelectionCallbacks(
        albums = selectedAlbums,
        appCallbacks = appCallbacks,
        onPlayClick = { viewModel.playAlbums(selectedAlbums) },
        onEnqueueClick = { viewModel.enqueueAlbums(selectedAlbums, context) },
        onUnselectAllClick = { viewModel.unselectAllAlbums() },
        onSelectAllClick = { viewModel.selectAlbums(albumCombos.map { it.album }) },
    )
    val progressIndicatorText =
        if (isImporting) stringResource(R.string.importing_local_albums)
        else if (isLoadingAlbums) stringResource(R.string.loading_albums)
        else null
    val onEmpty: @Composable () -> Unit = {
        if (!isImporting && !isLoadingAlbums) {
            Text(
                stringResource(R.string.no_albums_found),
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    ListActions(
        initialSearchTerm = searchTerm,
        sortParameter = sortParameter,
        sortOrder = sortOrder,
        sortParameters = AlbumSortParameter.withLabels(context),
        sortDialogTitle = stringResource(R.string.album_order),
        onSort = { param, order -> viewModel.setAlbumSorting(param, order) },
        onSearch = { viewModel.setAlbumSearchTerm(it) },
    )

    when (displayType) {
        DisplayType.LIST -> AlbumList(
            combos = albumCombos,
            albumCallbacks = albumCallbacks,
            albumSelectionCallbacks = albumSelectionCallbacks,
            selectedAlbums = selectedAlbums,
            onEmpty = onEmpty,
            albumDownloadTasks = albumDownloadTasks,
            progressIndicatorText = progressIndicatorText,
            getThumbnail = { viewModel.getAlbumThumbnail(it) },
        )
        DisplayType.GRID -> AlbumGrid(
            combos = albumCombos,
            albumCallbacks = albumCallbacks,
            selectedAlbums = selectedAlbums,
            albumSelectionCallbacks = albumSelectionCallbacks,
            onEmpty = onEmpty,
            albumDownloadTasks = albumDownloadTasks,
            progressIndicatorText = progressIndicatorText,
        )
    }
}
