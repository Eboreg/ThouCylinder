package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.persistentListOf
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenAlbumTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenArtistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenPlaylistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenTrackTab
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
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
    val albumViewStates by viewModel.albumViewStates.collectAsStateWithLifecycle(persistentListOf())
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val trackCombos: LazyPagingItems<TrackCombo> = viewModel.pagingTrackCombos.collectAsLazyPagingItems()
    var showToolbars by remember { mutableStateOf(true) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }

    Column(modifier = modifier) {
        when (listType) {
            ListType.ALBUMS -> LibraryScreenAlbumTab(
                viewModel = viewModel,
                appCallbacks = appCallbacks,
                viewStates = albumViewStates,
                isImporting = isImportingLocalMedia,
                displayType = displayType,
                showToolbars = showToolbars,
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
            )
            ListType.TRACKS -> LibraryScreenTrackTab(
                trackCombos = trackCombos,
                isImporting = isImportingLocalMedia,
                viewModel = viewModel,
                appCallbacks = appCallbacks,
                displayType = displayType,
                gridState = trackGridState,
                listState = trackListState,
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
                showToolbars = showToolbars,
            )
            ListType.ARTISTS -> LibraryScreenArtistTab(
                displayType = displayType,
                isImporting = isImportingLocalMedia,
                onArtistClick = appCallbacks.onArtistClick,
                showToolbars = showToolbars,
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
                onDisplayTypeChange = { viewModel.setDisplayType(it) },
                onListTypeChange = { viewModel.setListType(it) },
            )
            ListType.PLAYLISTS -> LibraryScreenPlaylistTab(
                viewModel = viewModel,
                appCallbacks = appCallbacks,
                showToolbars = showToolbars,
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
            )
        }
    }
}
