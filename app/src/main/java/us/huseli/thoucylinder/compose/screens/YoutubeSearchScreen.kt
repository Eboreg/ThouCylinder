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
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
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
import us.huseli.thoucylinder.dataclasses.abstr.tracks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current

    val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
    val albumCombos by viewModel.albumCombos.collectAsStateWithLifecycle(emptyList())
    val isSearchingAlbums by viewModel.isSearchingAlbums.collectAsStateWithLifecycle()
    val isSearchingTracks by viewModel.isSearchingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackCombo by viewModel.latestSelectedTrackCombo.collectAsStateWithLifecycle(null)
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedAlbumCombos by viewModel.selectedAlbumsWithTracks.collectAsStateWithLifecycle(emptyList())
    val selectedTrackCombos by viewModel.selectedTrackCombos.collectAsStateWithLifecycle(emptyList())
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackCombos = viewModel.trackCombos.collectAsLazyPagingItems()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var latestSelectedTrackIndex by rememberSaveable(selectedTrackCombos) { mutableStateOf<Int?>(null) }
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
                selectedAlbumCombos = selectedAlbumCombos,
                isSearching = isSearchingAlbums,
                albumDownloadTasks = albumDownloadTasks,
                getThumbnail = { viewModel.getAlbumThumbnail(it) },
                albumCallbacks = { combo: AlbumWithTracksCombo ->
                    AlbumCallbacks(
                        combo = combo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onPlayClick = { viewModel.playTrackCombos(combo.trackCombos) },
                        onEnqueueClick = { viewModel.enqueueTrackCombos(combo.trackCombos, context) },
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(albumWithTracks = combo)) },
                        onAlbumLongClick = {
                            viewModel.selectAlbumsFromLastSelected(combo.album, albumCombos.map { it.album })
                        },
                        onAlbumClick = {
                            if (selectedAlbumCombos.isNotEmpty()) viewModel.toggleSelected(combo.album)
                            else {
                                viewModel.updateFromMusicBrainz(combo)
                                appCallbacks.onAlbumClick(combo.album.albumId)
                            }
                        },
                    )
                },
                albumSelectionCallbacks = AlbumSelectionCallbacks(
                    onPlayClick = { viewModel.playTrackCombos(selectedAlbumCombos.flatMap { it.trackCombos }) },
                    onEnqueueClick = {
                        viewModel.enqueueTrackCombos(selectedAlbumCombos.flatMap { it.trackCombos }, context)
                    },
                    onUnselectAllClick = { viewModel.unselectAllAlbums() },
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(albumsWithTracks = selectedAlbumCombos))
                    },
                    onSelectAllClick = { viewModel.selectAlbums(albumCombos.map { it.album }) },
                ),
            )
            ListType.TRACKS -> TrackSearchResults(
                tracks = trackCombos,
                selectedTrackCombos = selectedTrackCombos,
                viewModel = viewModel,
                displayType = displayType,
                isSearching = isSearchingTracks,
                trackCallbacks = { index: Int, combo: TrackCombo ->
                    TrackCallbacks(
                        combo = combo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onEnqueueClick = { viewModel.enqueueTrackCombo(combo, context) },
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(track = combo.track)) },
                        onLongClick = {
                            viewModel.selectTrackCombos(
                                latestSelectedTrackIndex?.let { index2 ->
                                    (min(index, index2)..max(index, index2)).mapNotNull { trackCombos[it] }
                                } ?: listOf(combo)
                            )
                        },
                        onTrackClick = {
                            if (selectedTrackCombos.isNotEmpty()) viewModel.toggleSelected(combo)
                            else viewModel.playTrackCombo(combo)
                        },
                        onEach = {
                            if (combo.track.trackId == latestSelectedTrackCombo?.track?.trackId)
                                latestSelectedTrackIndex = index
                        },
                    )
                },
                trackDownloadTasks = trackDownloadTasks,
                trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackCombos.tracks()))
                    },
                    onPlayClick = { viewModel.playTrackCombos(selectedTrackCombos) },
                    onEnqueueClick = { viewModel.enqueueTrackCombos(selectedTrackCombos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackCombos() },
                ),
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
    albumCombos: List<AlbumWithTracksCombo>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumCombos: List<AlbumWithTracksCombo>,
    albumDownloadTasks: List<AlbumDownloadTask>,
    getThumbnail: suspend (Album) -> ImageBitmap?,
) {
    when (displayType) {
        DisplayType.LIST -> {
            AlbumList(
                combos = albumCombos,
                albumCallbacks = albumCallbacks,
                albumSelectionCallbacks = albumSelectionCallbacks,
                selectedAlbums = selectedAlbumCombos.map { it.album },
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
                selectedAlbums = selectedAlbumCombos.map { it.album },
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
    selectedTrackCombos: List<TrackCombo>,
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
                selectedTrackCombos = selectedTrackCombos,
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
                selectedTrackCombos = selectedTrackCombos,
                trackDownloadTasks = trackDownloadTasks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
    }
}
