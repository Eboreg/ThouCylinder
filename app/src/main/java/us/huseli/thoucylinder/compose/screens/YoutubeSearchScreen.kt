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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
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
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
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
    val scope = rememberCoroutineScope()

    val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())
    val isSearchingAlbums by viewModel.isSearchingAlbums.collectAsStateWithLifecycle()
    val isSearchingTracks by viewModel.isSearchingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackPojo by viewModel.latestSelectedTrackPojo.collectAsStateWithLifecycle(null)
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedAlbumPojos by viewModel.selectedAlbumsWithTracks.collectAsStateWithLifecycle(emptyList())
    val selectedTrackPojos by viewModel.selectedTrackPojos.collectAsStateWithLifecycle(emptyList())
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackPojos = viewModel.trackPojos.collectAsLazyPagingItems()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var latestSelectedTrackIndex by rememberSaveable(selectedTrackPojos) { mutableStateOf<Int?>(null) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    fun ensureTrackMetadata(pojos: List<TrackPojo>, callback: (List<TrackPojo>) -> Unit) =
        scope.launch { callback(viewModel.ensureTrackMetadata(pojos)) }

    fun ensureTrackMetadata(pojo: TrackPojo, callback: (TrackPojo) -> Unit) =
        ensureTrackMetadata(listOf(pojo)) { callback(it.first()) }

    fun ensureTrackMetadata(pojos: List<AlbumWithTracksPojo>, callback: (List<AlbumWithTracksPojo>) -> Unit) =
        scope.launch {
            callback(
                pojos.map { pojo ->
                    pojo.copy(tracks = viewModel.ensureTrackMetadata(pojo.trackPojos).map { it.track })
                }
            )
        }

    fun ensureTrackMetadata(pojo: AlbumWithTracksPojo, callback: (AlbumWithTracksPojo) -> Unit) =
        ensureTrackMetadata(listOf(pojo)) { callback(it.first()) }

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

        if (isSearching) {
            ObnoxiousProgressIndicator(modifier = Modifier.padding(10.dp), tonalElevation = 5.dp)
        }

        when (listType) {
            ListType.ALBUMS -> AlbumSearchResults(
                albumPojos = albumPojos,
                displayType = displayType,
                selectedAlbumPojos = selectedAlbumPojos,
                isSearching = isSearchingAlbums,
                albumDownloadTasks = albumDownloadTasks,
                albumCallbacks = { pojo: AlbumWithTracksPojo ->
                    AlbumCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onPlayClick = { ensureTrackMetadata(pojo) { viewModel.playTrackPojos(it.trackPojos) } },
                        onEnqueueClick = {
                            ensureTrackMetadata(pojo) { viewModel.enqueueTrackPojos(it.trackPojos, context) }
                        },
                        onAddToPlaylistClick = {
                            ensureTrackMetadata(pojo) {
                                appCallbacks.onAddToPlaylistClick(Selection(albumWithTracks = it))
                            }
                        },
                        onAlbumLongClick = {
                            viewModel.selectAlbumsFromLastSelected(pojo.album, albumPojos.map { it.album })
                        },
                        onAlbumClick = {
                            if (selectedAlbumPojos.isNotEmpty()) viewModel.toggleSelected(pojo.album)
                            else appCallbacks.onAlbumClick(pojo.album.albumId)
                        },
                    )
                },
                albumSelectionCallbacks = AlbumSelectionCallbacks(
                    onPlayClick = {
                        ensureTrackMetadata(selectedAlbumPojos.flatMap { it.trackPojos }) {
                            viewModel.playTrackPojos(it)
                        }
                    },
                    onEnqueueClick = {
                        ensureTrackMetadata(selectedAlbumPojos.flatMap { it.trackPojos }) {
                            viewModel.enqueueTrackPojos(it, context)
                        }
                    },
                    onUnselectAllClick = { viewModel.unselectAllAlbums() },
                    onAddToPlaylistClick = {
                        ensureTrackMetadata(selectedAlbumPojos) {
                            appCallbacks.onAddToPlaylistClick(Selection(albumsWithTracks = it))
                        }
                    },
                    onSelectAllClick = { viewModel.selectAlbums(albumPojos.map { it.album }) },
                ),
            )
            ListType.TRACKS -> TrackSearchResults(
                tracks = trackPojos,
                selectedTrackPojos = selectedTrackPojos,
                viewModel = viewModel,
                displayType = displayType,
                isSearching = isSearchingTracks,
                trackCallbacks = { index: Int, pojo: TrackPojo ->
                    TrackCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onEnqueueClick = { ensureTrackMetadata(pojo) { viewModel.enqueueTrackPojo(it, context) } },
                        onAddToPlaylistClick = {
                            ensureTrackMetadata(pojo) { appCallbacks.onAddToPlaylistClick(Selection(track = it.track)) }
                        },
                        onLongClick = {
                            viewModel.selectTrackPojos(
                                latestSelectedTrackIndex?.let { index2 ->
                                    (min(index, index2)..max(index, index2)).mapNotNull { trackPojos[it] }
                                } ?: listOf(pojo)
                            )
                        },
                        onTrackClick = {
                            ensureTrackMetadata(pojo) {
                                if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(it)
                                else viewModel.playTrackPojo(it)
                            }
                        },
                        onEach = {
                            if (pojo.track.trackId == latestSelectedTrackPojo?.track?.trackId)
                                latestSelectedTrackIndex = index
                        },
                    )
                },
                trackDownloadTasks = trackDownloadTasks,
                trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        ensureTrackMetadata(selectedTrackPojos) { pojos ->
                            appCallbacks.onAddToPlaylistClick(Selection(tracks = pojos.tracks()))
                        }
                    },
                    onPlayClick = { ensureTrackMetadata(selectedTrackPojos) { viewModel.playTrackPojos(it) } },
                    onEnqueueClick = {
                        ensureTrackMetadata(selectedTrackPojos) {
                            viewModel.enqueueTrackPojos(it, context)
                        }
                    },
                    onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
                ),
            )
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}


@Composable
fun AlbumSearchResults(
    albumCallbacks: (AlbumWithTracksPojo) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    albumPojos: List<AlbumWithTracksPojo>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumPojos: List<AlbumWithTracksPojo>,
    albumDownloadTasks: List<AlbumDownloadTask>,
) {
    when (displayType) {
        DisplayType.LIST -> {
            AlbumList(
                pojos = albumPojos,
                albumCallbacks = albumCallbacks,
                albumSelectionCallbacks = albumSelectionCallbacks,
                selectedAlbums = selectedAlbumPojos.map { it.album },
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                },
                albumDownloadTasks = albumDownloadTasks,
            )
        }
        DisplayType.GRID -> {
            AlbumGrid(
                pojos = albumPojos,
                albumCallbacks = albumCallbacks,
                selectedAlbums = selectedAlbumPojos.map { it.album },
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
    selectedTrackPojos: List<TrackPojo>,
    trackCallbacks: (Int, TrackPojo) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    tracks: LazyPagingItems<TrackPojo>,
    trackDownloadTasks: List<TrackDownloadTask>,
    viewModel: YoutubeSearchViewModel,
) {
    when (displayType) {
        DisplayType.LIST -> {
            TrackList(
                trackPojos = tracks,
                selectedTrackPojos = selectedTrackPojos,
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
                trackPojos = tracks,
                viewModel = viewModel,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                selectedTrackPojos = selectedTrackPojos,
                trackDownloadTasks = trackDownloadTasks,
                onEmpty = {
                    if (!isSearching) Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
    }
}
