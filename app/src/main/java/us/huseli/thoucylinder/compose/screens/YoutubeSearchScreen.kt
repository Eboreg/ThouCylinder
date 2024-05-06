package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListDisplayTypeButton
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.ListTypeChips
import us.huseli.thoucylinder.compose.YoutubeSearchForm
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
    albumCallbacks: AlbumCallbacks,
    trackCallbacks: TrackCallbacks,
) {
    val context = LocalContext.current

    val albumUiStates by viewModel.albumUiStates.collectAsStateWithLifecycle()
    val isSearchingAlbums by viewModel.isSearchingAlbums.collectAsStateWithLifecycle()
    val isSearchingTracks by viewModel.isSearchingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val trackUiStates = viewModel.trackUiStates.collectAsLazyPagingItems()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var latestSelectedTrackIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }
    var showToolbars by remember { mutableStateOf(true) }
    val nestedScrollConnection = rememberToolbarScrollConnection { showToolbars = it }

    val isSearching = when (listType) {
        ListType.ALBUMS -> isSearchingAlbums
        ListType.TRACKS -> isSearchingTracks
        ListType.ARTISTS -> false
        ListType.PLAYLISTS -> false
    }

    Column(modifier = modifier) {
        CollapsibleToolbar(show = { showToolbars }) {
            YoutubeSearchForm(
                isSearching = isSearching,
                initialQuery = query,
                onSearch = { viewModel.search(it) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListTypeChips(
                        current = listType,
                        onChange = { listType = it },
                        exclude = persistentListOf(ListType.ARTISTS, ListType.PLAYLISTS),
                    )
                }
                ListDisplayTypeButton(current = displayType, onChange = { displayType = it })
            }
        }

        if (isSearching) ObnoxiousProgressIndicator()

        when (listType) {
            ListType.ALBUMS -> AlbumSearchResults(
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                states = { albumUiStates },
                displayType = displayType,
                selectedAlbumIds = selectedAlbumIds.toImmutableList(),
                isSearching = isSearchingAlbums,
                selectionCallbacks = viewModel.getAlbumSelectionCallbacks(appCallbacks, context),
                callbacks = remember {
                    albumCallbacks.copy(
                        onPlayClick = { viewModel.playAlbum(it) },
                        onEnqueueClick = { viewModel.enqueueAlbum(it) },
                        onAlbumLongClick = { albumId -> viewModel.selectAlbumsFromLastSelected(to = albumId) },
                        onAlbumClick = { albumId ->
                            if (selectedAlbumIds.isNotEmpty()) viewModel.toggleAlbumSelected(albumId)
                            else {
                                viewModel.updateFromMusicBrainz(albumId)
                                albumCallbacks.onAlbumClick?.invoke(albumId)
                            }
                        }
                    )
                },
            )
            ListType.TRACKS -> TrackSearchResults(
                displayType = displayType,
                isSearching = isSearchingTracks,
                selectedTrackIds = selectedTrackIds.toImmutableList(),
                trackCallbacks = { index: Int, state: TrackUiState ->
                    trackCallbacks.copy(
                        onLongClick = {
                            viewModel.selectTracksBetweenIndices(
                                fromIndex = latestSelectedTrackIndex,
                                toIndex = index,
                                getTrackIdAtIndex = { trackUiStates[it]?.trackId },
                            )
                        },
                        onTrackClick = {
                            if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(state.trackId)
                            else if (state.isPlayable) viewModel.playTrack(state)
                        },
                        onEach = {
                            if (state.trackId == latestSelectedTrackId) latestSelectedTrackIndex = index
                        },
                    )
                },
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks),
                trackUiStates = trackUiStates,
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                viewModel = viewModel,
            )
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}


@Composable
fun AlbumSearchResults(
    callbacks: AlbumCallbacks,
    selectionCallbacks: AlbumSelectionCallbacks,
    states: () -> ImmutableList<AlbumUiState>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumIds: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    when (displayType) {
        DisplayType.LIST -> {
            AlbumList(
                states = states,
                callbacks = callbacks,
                selectionCallbacks = selectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
                modifier = modifier,
            )
        }
        DisplayType.GRID -> {
            AlbumGrid(
                states = states,
                callbacks = callbacks,
                selectionCallbacks = selectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                modifier = modifier,
            ) {
                if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
            }
        }
    }
}


@Composable
fun TrackSearchResults(
    displayType: DisplayType,
    isSearching: Boolean,
    selectedTrackIds: ImmutableList<String>,
    trackCallbacks: (Int, TrackUiState) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    trackUiStates: LazyPagingItems<TrackUiState>,
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
) {
    when (displayType) {
        DisplayType.LIST -> {
            TrackList(
                uiStates = trackUiStates,
                trackCallbacks = trackCallbacks,
                selectedTrackIds = selectedTrackIds,
                trackSelectionCallbacks = trackSelectionCallbacks,
                modifier = modifier,
                ensureTrackMetadata = { viewModel.ensureTrackMetadata(it) },
            ) {
                if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
            }
        }
        DisplayType.GRID -> {
            TrackGrid(
                uiStates = trackUiStates,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                selectedTrackIds = selectedTrackIds,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
                modifier = modifier,
                ensureTrackMetadata = { viewModel.ensureTrackMetadata(it) },
            )
        }
    }
}
