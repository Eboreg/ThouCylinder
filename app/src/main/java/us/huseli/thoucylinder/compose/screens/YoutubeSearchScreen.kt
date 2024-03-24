package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
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
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel
import java.util.UUID

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current

    val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
    val albumCombos by viewModel.albumCombos.collectAsStateWithLifecycle(persistentListOf())
    val isSearchingAlbums by viewModel.isSearchingAlbums.collectAsStateWithLifecycle()
    val isSearchingTracks by viewModel.isSearchingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle(null)
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedAlbumIds by viewModel.filteredSelectedAlbumIds.collectAsStateWithLifecycle(emptyList())
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle(emptyList())
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackCombos = viewModel.trackCombos.collectAsLazyPagingItems()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var latestSelectedTrackIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    val isSearching = when (listType) {
        ListType.ALBUMS -> isSearchingAlbums
        ListType.TRACKS -> isSearchingTracks
        ListType.ARTISTS -> false
        ListType.PLAYLISTS -> false
    }

    Column(modifier = modifier) {
        Surface(color = BottomAppBarDefaults.containerColor, tonalElevation = 2.dp) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 10.dp).padding(horizontal = 10.dp),
            ) {
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
        }

        if (isSearching) ObnoxiousProgressIndicator()

        when (listType) {
            ListType.ALBUMS -> AlbumSearchResults(
                albumCombos = albumCombos,
                displayType = displayType,
                selectedAlbumIds = selectedAlbumIds.toImmutableList(),
                isSearching = isSearchingAlbums,
                albumDownloadTasks = albumDownloadTasks.toImmutableList(),
                getThumbnail = { viewModel.getAlbumThumbnail(it, context) },
                albumCallbacks = { combo: AlbumWithTracksCombo ->
                    AlbumCallbacks(
                        combo = combo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onPlayClick = { viewModel.playTrackCombos(combo.trackCombos) },
                        onEnqueueClick = { viewModel.enqueueTrackCombos(combo.trackCombos, context) },
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(albumWithTracks = combo)) },
                        onAlbumLongClick = {
                            viewModel.selectAlbumsFromLastSelected(
                                to = combo.album.albumId,
                                allAlbumIds = albumCombos.map { it.album.albumId },
                            )
                        },
                        onAlbumClick = {
                            if (selectedAlbumIds.isNotEmpty()) viewModel.toggleAlbumSelected(combo.album.albumId)
                            else {
                                viewModel.updateFromMusicBrainzAsync(combo)
                                appCallbacks.onAlbumClick(combo.album.albumId)
                            }
                        },
                    )
                },
                albumSelectionCallbacks = viewModel.getAlbumSelectionCallbacks(appCallbacks, context),
            )
            ListType.TRACKS -> TrackSearchResults(
                tracks = trackCombos,
                selectedTrackIds = selectedTrackIds,
                viewModel = viewModel,
                displayType = displayType,
                isSearching = isSearchingTracks,
                trackCallbacks = { index: Int, combo: TrackCombo ->
                    TrackCallbacks(
                        combo = combo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onEnqueueClick = if (combo.track.isPlayable) {
                            { viewModel.enqueueTrackCombo(combo, context) }
                        } else null,
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(track = combo.track)) },
                        onLongClick = {
                            viewModel.selectTracksBetweenIndices(
                                fromIndex = latestSelectedTrackIndex,
                                toIndex = index,
                                getTrackIdAtIndex = { trackCombos[it]?.track?.trackId },
                            )
                        },
                        onTrackClick = {
                            if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(combo.track.trackId)
                            else if (combo.track.isPlayable) viewModel.playTrackCombo(combo)
                        },
                        onEach = {
                            if (combo.track.trackId == latestSelectedTrackId) latestSelectedTrackIndex = index
                        },
                    )
                },
                trackDownloadTasks = trackDownloadTasks,
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks, context),
            )
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}


@Composable
fun AlbumSearchResults(
    albumCallbacks: (AlbumWithTracksCombo) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    albumCombos: ImmutableList<AlbumWithTracksCombo>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumIds: ImmutableList<UUID>,
    albumDownloadTasks: ImmutableList<AlbumDownloadTask>,
    getThumbnail: suspend (AlbumWithTracksCombo) -> ImageBitmap?,
) {
    when (displayType) {
        DisplayType.LIST -> {
            AlbumList(
                combos = albumCombos,
                albumCallbacks = albumCallbacks,
                albumSelectionCallbacks = albumSelectionCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
                albumDownloadTasks = albumDownloadTasks,
                getThumbnail = getThumbnail,
            )
        }
        DisplayType.GRID -> {
            AlbumGrid(
                combos = albumCombos,
                albumCallbacks = albumCallbacks,
                selectedAlbumIds = selectedAlbumIds,
                albumSelectionCallbacks = albumSelectionCallbacks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
                albumDownloadTasks = albumDownloadTasks,
            )
        }
    }
}


@Composable
fun TrackSearchResults(
    displayType: DisplayType,
    isSearching: Boolean,
    selectedTrackIds: List<UUID>,
    trackCallbacks: (Int, TrackCombo) -> TrackCallbacks<TrackCombo>,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    tracks: LazyPagingItems<TrackCombo>,
    trackDownloadTasks: List<TrackDownloadTask>,
    viewModel: YoutubeSearchViewModel,
) {
    when (displayType) {
        DisplayType.LIST -> {
            TrackList(
                trackCombos = tracks,
                selectedTrackIds = selectedTrackIds,
                viewModel = viewModel,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                trackDownloadTasks = trackDownloadTasks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
        DisplayType.GRID -> {
            TrackGrid(
                trackCombos = tracks,
                viewModel = viewModel,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                selectedTrackIds = selectedTrackIds,
                trackDownloadTasks = trackDownloadTasks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
    }
}
