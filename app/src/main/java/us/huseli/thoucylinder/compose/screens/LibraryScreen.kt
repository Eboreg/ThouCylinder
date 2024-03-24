package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenAlbumTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenArtistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenPlaylistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenTrackTab
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    trackListState: LazyListState = rememberLazyListState(),
    trackGridState: LazyGridState = rememberLazyGridState(),
    appCallbacks: AppCallbacks,
) {
    val albumCombos by viewModel.albumCombos.collectAsStateWithLifecycle(emptyList())
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val trackCombos: LazyPagingItems<TrackCombo> = viewModel.pagingTrackCombos.collectAsLazyPagingItems()

    val availableDisplayTypes =
        if (listType == ListType.PLAYLISTS) listOf(DisplayType.LIST)
        else listOf(DisplayType.LIST, DisplayType.GRID)

    Column(modifier = modifier) {
        ListSettingsRow(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = availableDisplayTypes,
        )

        when (listType) {
            ListType.ALBUMS -> LibraryScreenAlbumTab(
                viewModel = viewModel,
                appCallbacks = appCallbacks,
                albumCombos = albumCombos.toImmutableList(),
                isImporting = isImportingLocalMedia,
                displayType = displayType,
            )
            ListType.TRACKS -> LibraryScreenTrackTab(
                trackCombos = trackCombos,
                isImporting = isImportingLocalMedia,
                viewModel = viewModel,
                appCallbacks = appCallbacks,
                displayType = displayType,
                gridState = trackGridState,
                listState = trackListState,
            )
            ListType.ARTISTS -> LibraryScreenArtistTab(
                displayType = displayType,
                isImporting = isImportingLocalMedia,
                onArtistClick = appCallbacks.onArtistClick,
            )
            ListType.PLAYLISTS -> LibraryScreenPlaylistTab(
                viewModel = viewModel,
                appCallbacks = appCallbacks,
            )
        }
    }
}
