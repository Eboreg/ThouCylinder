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
import us.huseli.thoucylinder.Selection
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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel

@Composable
fun YoutubeSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeSearchViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
    val isSearchingYoutubeAlbums by viewModel.isSearchingYoutubeAlbums.collectAsStateWithLifecycle()
    val isSearchingYoutubeTracks by viewModel.isSearchingYoutubeTracks.collectAsStateWithLifecycle()
    val selectedYoutubeAlbumPojos by viewModel.selectedYoutubeAlbumPojos.collectAsStateWithLifecycle(emptyList())
    val selectedYoutubeTrackPojos by viewModel.selectedYoutubeTrackPojos.collectAsStateWithLifecycle(emptyList())
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val youtubeAlbums by viewModel.youtubeAlbumPojos.collectAsStateWithLifecycle(emptyList())
    val youtubeTracks = viewModel.youtubeTrackPojos.collectAsLazyPagingItems()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var displayType by rememberSaveable { mutableStateOf(DisplayType.LIST) }
    var listType by rememberSaveable { mutableStateOf(ListType.ALBUMS) }

    val withTempTrackPojos: (List<TrackPojo>, (List<TrackPojo>) -> Unit) -> (() -> Unit) = { pojos, callback ->
        { scope.launch { callback(viewModel.ensureTrackMetadata(pojos)) } }
    }

    val withTempTrackPojo: (TrackPojo, (TrackPojo) -> Unit) -> (() -> Unit) = { track, callback ->
        withTempTrackPojos(listOf(track)) { callback(it.first()) }
    }

    val isSearching = when (listType) {
        ListType.ALBUMS -> isSearchingYoutubeAlbums
        ListType.TRACKS -> isSearchingYoutubeTracks
        ListType.ARTISTS -> false
        ListType.PLAYLISTS -> false
    }

    Column(modifier = modifier) {
        SearchForm(
            modifier = Modifier.padding(horizontal = 10.dp),
            isSearching = isSearching,
            initialQuery = query,
            onSearch = { viewModel.search(it) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ListTypeChips(
                    current = listType,
                    onChange = { listType = it },
                    exclude = listOf(ListType.ARTISTS, ListType.PLAYLISTS),
                )
            }
            ListDisplayTypeButton(current = displayType, onChange = { displayType = it })
        }

        if (isSearching) {
            ObnoxiousProgressIndicator(modifier = Modifier.padding(10.dp), tonalElevation = 5.dp)
        }

        when (listType) {
            ListType.ALBUMS -> AlbumSearchResults(
                albumPojos = youtubeAlbums,
                displayType = displayType,
                selectedAlbumPojos = selectedYoutubeAlbumPojos,
                isSearching = isSearchingYoutubeAlbums,
                albumDownloadTasks = albumDownloadTasks,
                albumCallbacks = { pojo: AbstractAlbumPojo ->
                    AlbumCallbacks.fromAppCallbacks(
                        album = pojo.album,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playTrackPojos((pojo as AlbumWithTracksPojo).trackPojos) },
                        onEnqueueClick = {
                            viewModel.enqueueTrackPojos((pojo as AlbumWithTracksPojo).trackPojos, context)
                        },
                        onAddToPlaylistClick = {
                            appCallbacks.onAddToPlaylistClick(
                                Selection(tracks = (pojo as AlbumWithTracksPojo).tracks)
                            )
                        },
                        onAlbumLongClick = {
                            viewModel.selectYoutubeAlbumPojosFromLastSelected(pojo as AlbumWithTracksPojo)
                        },
                        onAlbumClick = {
                            if (selectedYoutubeAlbumPojos.isNotEmpty())
                                viewModel.toggleSelectedYoutube(pojo as AlbumWithTracksPojo)
                            else appCallbacks.onAlbumClick(pojo.album.albumId)
                        },
                    )
                },
                albumSelectionCallbacks = AlbumSelectionCallbacks(
                    onPlayClick = {
                        viewModel.playTrackPojos(selectedYoutubeAlbumPojos.flatMap { it.trackPojos })
                    },
                    onEnqueueClick = {
                        viewModel.enqueueTrackPojos(
                            selectedYoutubeAlbumPojos.flatMap { it.trackPojos },
                            context,
                        )
                    },
                    onUnselectAllClick = { viewModel.unselectAllYoutubeAlbumPojos() },
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(
                            Selection(tracks = selectedYoutubeAlbumPojos.flatMap { it.tracks })
                        )
                    },
                    onSelectAllClick = { viewModel.selectAllYoutubeAlbumPojos() },
                ),
            )
            ListType.TRACKS -> TrackSearchResults(
                tracks = youtubeTracks,
                selectedTrackPojos = selectedYoutubeTrackPojos,
                viewModel = viewModel,
                displayType = displayType,
                isSearching = isSearchingYoutubeTracks,
                trackCallbacks = { pojo: TrackPojo ->
                    TrackCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        onEnqueueClick = withTempTrackPojo(pojo) { viewModel.enqueueTrackPojo(it, context) },
                        onAddToPlaylistClick = withTempTrackPojo(pojo) {
                            appCallbacks.onAddToPlaylistClick(Selection(trackPojo = it))
                        },
                        onLongClick = { viewModel.toggleSelectedYoutube(pojo) },
                        onTrackClick = withTempTrackPojo(pojo) {
                            if (selectedYoutubeAlbumPojos.isNotEmpty()) viewModel.toggleSelectedYoutube(it)
                            else viewModel.playTrackPojo(it)
                        },
                    )
                },
                trackDownloadTasks = trackDownloadTasks,
                trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = withTempTrackPojos(selectedYoutubeTrackPojos) {
                        appCallbacks.onAddToPlaylistClick(Selection(trackPojos = it))
                    },
                    onPlayClick = withTempTrackPojos(selectedYoutubeTrackPojos) { viewModel.playTrackPojos(it) },
                    onEnqueueClick = withTempTrackPojos(selectedYoutubeTrackPojos) {
                        viewModel.enqueueTrackPojos(it, context)
                    },
                    onUnselectAllClick = { viewModel.unselectAllYoutubeTrackPojos() },
                ),
            )
            ListType.ARTISTS -> {}
            ListType.PLAYLISTS -> {}
        }
    }
}


@Composable
fun AlbumSearchResults(
    albumCallbacks: (AbstractAlbumPojo) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    albumPojos: List<AbstractAlbumPojo>,
    displayType: DisplayType,
    isSearching: Boolean,
    selectedAlbumPojos: List<AbstractAlbumPojo>,
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
    trackCallbacks: (TrackPojo) -> TrackCallbacks,
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
                    if (!isSearching)
                        Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
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
                    if (!isSearching)
                        Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                },
            )
        }
    }
}
