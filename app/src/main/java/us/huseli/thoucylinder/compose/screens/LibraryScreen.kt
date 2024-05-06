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
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenAlbumTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenArtistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenPlaylistTab
import us.huseli.thoucylinder.compose.screens.library.LibraryScreenTrackTab
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
    albumCallbacks: AlbumCallbacks,
    trackCallbacks: TrackCallbacks,
) {
    val albumUiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val uiStates: LazyPagingItems<TrackUiState> = viewModel.pagingTrackUiStates.collectAsLazyPagingItems()
    var showToolbars by remember { mutableStateOf(true) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }
    val trackListState: LazyListState = rememberLazyListState()
    val trackGridState: LazyGridState = rememberLazyGridState()

    Column {
        when (listType) {
            ListType.ALBUMS -> LibraryScreenAlbumTab(
                viewModel = viewModel,
                uiStates = { albumUiStates },
                isImporting = isImportingLocalMedia,
                displayType = displayType,
                showToolbars = { showToolbars },
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
                appCallbacks = appCallbacks,
                albumCallbacks = albumCallbacks,
            )
            ListType.TRACKS -> LibraryScreenTrackTab(
                uiStates = uiStates,
                appCallbacks = appCallbacks,
                trackCallbacks = trackCallbacks,
                listState = trackListState,
                gridState = trackGridState,
                isImporting = isImportingLocalMedia,
                displayType = displayType,
                showToolbars = { showToolbars },
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
                viewModel = viewModel,
            )
            ListType.ARTISTS -> LibraryScreenArtistTab(
                displayType = displayType,
                isImporting = isImportingLocalMedia,
                onArtistClick = appCallbacks.onArtistClick,
                showToolbars = { showToolbars },
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
                onDisplayTypeChange = remember { { viewModel.setDisplayType(it) } },
                onListTypeChange = remember { { viewModel.setListType(it) } },
            )
            ListType.PLAYLISTS -> LibraryScreenPlaylistTab(
                viewModel = viewModel,
                appCallbacks = appCallbacks,
                showToolbars = { showToolbars },
                listModifier = Modifier.nestedScroll(nestedScrollConnection),
            )
        }
    }
}
