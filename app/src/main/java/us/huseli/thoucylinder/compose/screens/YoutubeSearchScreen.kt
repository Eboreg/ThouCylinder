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
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListDisplayTypeButton
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.ListTypeChips
import us.huseli.thoucylinder.compose.SearchForm
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.rememberToolbarScrollConnection
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current

    val albumDownloadStates by viewModel.albumDownloadStates.collectAsStateWithLifecycle()
    val albumViewStates by viewModel.albumViewStates.collectAsStateWithLifecycle(persistentListOf())
    val isSearchingAlbums by viewModel.isSearchingAlbums.collectAsStateWithLifecycle()
    val isSearchingTracks by viewModel.isSearchingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle(null)
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle(emptyList())
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle(emptyList())
    val trackDownloadStates by viewModel.trackDownloadStates.collectAsStateWithLifecycle()
    val trackCombos = viewModel.trackCombos.collectAsLazyPagingItems()

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
        CollapsibleToolbar(show = showToolbars) {
            SearchForm(
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
                        exclude = listOf(ListType.ARTISTS, ListType.PLAYLISTS),
                    )
                }
                ListDisplayTypeButton(current = displayType, onChange = { displayType = it })
            }
        }

        if (isSearching) ObnoxiousProgressIndicator()

        when (listType) {
            ListType.ALBUMS -> AlbumSearchResults(
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                states = albumViewStates,
                displayType = displayType,
                selectedAlbumIds = selectedAlbumIds.toImmutableList(),
                isSearching = isSearchingAlbums,
                callbacks = { state: Album.ViewState ->
                    AlbumCallbacks(
                        state = state,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playAlbum(state.album.albumId) },
                        onEnqueueClick = { viewModel.enqueueAlbum(state.album.albumId, context) },
                        onAddToPlaylistClick = {
                            viewModel.onAlbumTracks(state.album.albumId) {
                                appCallbacks.onAddToPlaylistClick(Selection(tracks = it))
                            }
                        },
                        onAlbumLongClick = {
                            viewModel.selectAlbumsFromLastSelected(
                                to = state.album.albumId,
                                allAlbumIds = albumViewStates.map { it.album.albumId },
                            )
                        },
                        onAlbumClick = {
                            if (selectedAlbumIds.isNotEmpty()) viewModel.toggleAlbumSelected(state.album.albumId)
                            else {
                                viewModel.updateFromMusicBrainz(state.album.albumId)
                                appCallbacks.onAlbumClick(state.album.albumId)
                            }
                        },
                    )
                },
                selectionCallbacks = viewModel.getAlbumSelectionCallbacks(appCallbacks, context),
                downloadStates = albumDownloadStates,
            )
            ListType.TRACKS -> TrackSearchResults(
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                tracks = trackCombos,
                selectedTrackIds = selectedTrackIds.toImmutableList(),
                viewModel = viewModel,
                displayType = displayType,
                isSearching = isSearchingTracks,
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks, context),
                downloadStates = trackDownloadStates,
                trackCallbacks = { index: Int, state: Track.ViewState ->
                    TrackCallbacks(
                        state = state,
                        appCallbacks = appCallbacks,
                        onEnqueueClick = if (state.track.isPlayable) {
                            { viewModel.enqueueTrack(state, context) }
                        } else null,
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(track = state.track)) },
                        onLongClick = {
                            viewModel.selectTracksBetweenIndices(
                                fromIndex = latestSelectedTrackIndex,
                                toIndex = index,
                                getTrackIdAtIndex = { trackCombos[it]?.track?.trackId },
                            )
                        },
                        onTrackClick = {
                            if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(state.track.trackId)
                            else if (state.track.isPlayable) viewModel.playTrack(state)
                        },
                        onEach = {
                            if (state.track.trackId == latestSelectedTrackId) latestSelectedTrackIndex = index
                        },
                    )
                },
            )
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}


@Composable
fun AlbumSearchResults(
    callbacks: (Album.ViewState) -> AlbumCallbacks,
    selectionCallbacks: AlbumSelectionCallbacks,
    states: ImmutableList<Album.ViewState>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumIds: ImmutableList<String>,
    downloadStates: ImmutableList<AlbumDownloadTask.ViewState>,
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
                downloadStates = downloadStates,
            )
        }
        DisplayType.GRID -> {
            AlbumGrid(
                states = states,
                callbacks = callbacks,
                selectedAlbumIds = selectedAlbumIds,
                selectionCallbacks = selectionCallbacks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
                modifier = modifier,
                downloadStates = downloadStates,
            )
        }
    }
}


@Composable
fun TrackSearchResults(
    displayType: DisplayType,
    isSearching: Boolean,
    selectedTrackIds: ImmutableList<String>,
    trackCallbacks: (Int, Track.ViewState) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    tracks: LazyPagingItems<TrackCombo>,
    downloadStates: ImmutableList<TrackDownloadTask.ViewState>,
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
) {
    when (displayType) {
        DisplayType.LIST -> {
            TrackList(
                trackCombos = tracks,
                selectedTrackIds = selectedTrackIds,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
                modifier = modifier,
                downloadStates = downloadStates,
                ensureTrackMetadata = { viewModel.ensureTrackMetadataAsync(it) },
            )
        }
        DisplayType.GRID -> {
            TrackGrid(
                trackCombos = tracks,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                selectedTrackIds = selectedTrackIds,
                downloadStates = downloadStates,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
                modifier = modifier,
                ensureTrackMetadata = { viewModel.ensureTrackMetadataAsync(it) },
            )
        }
    }
}
