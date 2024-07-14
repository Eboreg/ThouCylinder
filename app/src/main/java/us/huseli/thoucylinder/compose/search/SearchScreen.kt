package us.huseli.thoucylinder.compose.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListDisplayTypeButton
import us.huseli.thoucylinder.compose.album.AlbumCollection
import us.huseli.thoucylinder.compose.track.TrackCollection
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.externalcontent.ExternalListType
import us.huseli.thoucylinder.externalcontent.SearchCapability
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ExternalSearchViewModel

@Composable
fun SearchScreen(modifier: Modifier = Modifier, viewModel: ExternalSearchViewModel = hiltViewModel()) {
    val albumCallbacks = LocalAlbumCallbacks.current
    val albumUiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
    val backendKey by viewModel.backendKey.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val hasNextPage by viewModel.hasNextPage.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val listType: ExternalListType by viewModel.listType.collectAsStateWithLifecycle()
    val searchCapabilities: List<SearchCapability> by viewModel.searchCapabilities.collectAsStateWithLifecycle()
    val searchParams by viewModel.searchParams.collectAsStateWithLifecycle()
    val selectedAlbumCount = viewModel.selectedAlbumCount.collectAsStateWithLifecycle().asIntState()
    val selectedTrackCount = viewModel.selectedTrackCount.collectAsStateWithLifecycle().asIntState()
    val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()

    var showToolbars by remember { mutableStateOf(true) }
    var displayType by remember { mutableStateOf(DisplayType.LIST) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }
    val albumCallbacksWithHook = remember { viewModel.addAlbumCallbacksSaveHook(albumCallbacks) }

    LaunchedEffect(backendKey, listType) {
        viewModel.initBackend()
    }

    Column(modifier = modifier) {
        CollapsibleToolbar(
            show = { showToolbars },
            modifier = Modifier.padding(top = if (isInLandscapeMode()) 10.dp else 0.dp),
        ) {
            BackendSelectionSection(
                current = backendKey,
                onSelect = remember { { viewModel.setBackend(it) } },
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListTypeSection(
                    current = listType,
                    onSelect = remember { { viewModel.setListType(it) } },
                )
                PaginationSection(
                    hasPrevious = currentPage > 0,
                    hasNext = hasNextPage,
                    onPreviousClick = remember { { viewModel.gotoPreviousPage() } },
                    onNextClick = remember { { viewModel.gotoNextPage() } },
                )
                ListDisplayTypeButton(current = displayType, onChange = { displayType = it })
            }

            SearchFieldSection(
                capabilities = searchCapabilities,
                currentValues = searchParams,
                onSearch = remember { { viewModel.setSearchParams(it) } },
                listType = listType,
            )
        }

        when (listType) {
            ExternalListType.ALBUMS -> {
                CompositionLocalProvider(LocalAlbumCallbacks provides albumCallbacksWithHook) {
                    AlbumCollection(
                        displayType = displayType,
                        onClick = { viewModel.onAlbumClick(it, albumCallbacksWithHook.onGotoAlbumClick) },
                        onLongClick = { viewModel.onAlbumLongClick(it) },
                        selectedAlbumCount = { selectedAlbumCount.intValue },
                        selectionCallbacks = remember { viewModel.getAlbumSelectionCallbacks(dialogCallbacks) },
                        states = { albumUiStates },
                        downloadStateFlow = { viewModel.getAlbumDownloadUiStateFlow(it) },
                        modifier = Modifier.nestedScroll(nestedScrollConnection),
                        showArtist = true,
                        isLoading = isSearching,
                        onEmpty = { if (isEmpty) Text(stringResource(R.string.no_albums_found)) },
                    )
                }
            }
            ExternalListType.TRACKS -> TrackCollection(
                states = { trackUiStates },
                displayType = displayType,
                getDownloadStateFlow = remember { { viewModel.getTrackDownloadUiStateFlow(it) } },
                onClick = { viewModel.onTrackClick(it) },
                onLongClick = { viewModel.onTrackLongClick(it.id) },
                selectedTrackCount = { selectedTrackCount.intValue },
                trackSelectionCallbacks = remember { viewModel.getTrackSelectionCallbacks(dialogCallbacks) },
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                isLoading = isSearching,
                onEmpty = { if (isEmpty) Text(stringResource(R.string.no_tracks_found)) },
            )
        }
    }
}
