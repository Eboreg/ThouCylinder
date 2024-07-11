package us.huseli.thoucylinder.compose.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarListState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val albumUiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val isLocalMediaDirConfigured by viewModel.isLocalMediaDirConfigured.collectAsStateWithLifecycle()
    val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()

    var isRefreshPulled by remember { mutableStateOf(false) }
    var showToolbars by remember { mutableStateOf(true) }
    val isRefreshing = remember(isRefreshPulled, isImportingLocalMedia) { isRefreshPulled || isImportingLocalMedia }

    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }
    val trackListState: ScrollbarListState = rememberScrollbarListState()
    val trackGridState: ScrollbarGridState = rememberScrollbarGridState()
    val refreshState = rememberPullToRefreshState()

    if (isRefreshPulled && isImportingLocalMedia) isRefreshPulled = false

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshPulled = true
            viewModel.importNewLocalAlbums()
        },
        state = refreshState,
        indicator = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = refreshState.distanceFraction * 140.dp.roundToPx() - size.height
                    }
            ) {
                if (isLocalMediaDirConfigured) {
                    if (isImportingLocalMedia) {
                        ObnoxiousProgressIndicator(text = stringResource(R.string.importing_new_local_media_scream))
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = PullToRefreshDefaults.containerColor,
                            contentColor = PullToRefreshDefaults.indicatorColor,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.Refresh,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).padding(8.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            CollapsibleToolbar(
                show = { showToolbars },
                modifier = Modifier.padding(top = if (isInLandscapeMode()) 10.dp else 0.dp),
            ) {
                ListSettingsRow(
                    currentDisplayType = displayType,
                    currentListType = listType,
                    onDisplayTypeChange = remember { { viewModel.setDisplayType(it) } },
                    onListTypeChange = remember { { viewModel.setListType(it) } },
                )
                when (listType) {
                    ListType.ALBUMS -> AlbumListActions(viewModel = viewModel)
                    ListType.TRACKS -> TrackListActions(viewModel = viewModel)
                    ListType.ARTISTS -> ArtistListActions()
                    ListType.PLAYLISTS -> {}
                }
            }

            when (listType) {
                ListType.ALBUMS -> LibraryScreenAlbumTab(
                    uiStates = { albumUiStates },
                    modifier = Modifier.nestedScroll(nestedScrollConnection).fillMaxSize(),
                    displayType = displayType,
                    viewModel = viewModel,
                )
                ListType.TRACKS -> LibraryScreenTrackTab(
                    uiStates = { trackUiStates },
                    modifier = Modifier.nestedScroll(nestedScrollConnection).fillMaxSize(),
                    listState = trackListState,
                    gridState = trackGridState,
                    displayType = displayType,
                    viewModel = viewModel,
                )
                ListType.ARTISTS -> LibraryScreenArtistTab(
                    modifier = Modifier.nestedScroll(nestedScrollConnection).fillMaxSize(),
                    displayType = displayType,
                )
                ListType.PLAYLISTS -> LibraryScreenPlaylistTab(
                    displayType = displayType,
                    modifier = Modifier.nestedScroll(nestedScrollConnection).fillMaxSize(),
                )
            }
        }
    }
}
