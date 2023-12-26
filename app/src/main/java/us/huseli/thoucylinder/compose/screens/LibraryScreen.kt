package us.huseli.thoucylinder.compose.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.compose.ArtistGrid
import us.huseli.thoucylinder.compose.ArtistList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.PlaylistList
import us.huseli.thoucylinder.compose.album.AlbumGrid
import us.huseli.thoucylinder.compose.album.AlbumList
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.tracks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.LibraryViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    trackListState: LazyListState = rememberLazyListState(),
    trackGridState: LazyGridState = rememberLazyGridState(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val availableDisplayTypes =
        if (listType == ListType.PLAYLISTS) listOf(DisplayType.LIST)
        else listOf(DisplayType.LIST, DisplayType.GRID)

    Column(modifier = modifier) {
        ListSettingsRow(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = availableDisplayTypes,
        )

        when (listType) {
            ListType.ALBUMS -> {
                val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
                val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())
                val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
                val searchTerm by viewModel.albumSearchTerm.collectAsStateWithLifecycle()
                val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
                val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
                val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()

                val albumCallbacks = { pojo: AlbumPojo ->
                    AlbumCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onPlayClick = { viewModel.playAlbum(pojo.album) },
                        onEnqueueClick = { viewModel.enqueueAlbum(pojo.album, context) },
                        onAlbumLongClick = {
                            viewModel.selectAlbumsFromLastSelected(pojo.album, albumPojos.map { it.album })
                        },
                        onAlbumClick = {
                            if (selectedAlbums.isNotEmpty()) viewModel.toggleSelected(pojo.album)
                            else appCallbacks.onAlbumClick(pojo.album.albumId)
                        },
                    )
                }
                val albumSelectionCallbacks = AlbumSelectionCallbacks(
                    albums = selectedAlbums,
                    appCallbacks = appCallbacks,
                    onPlayClick = { viewModel.playAlbums(selectedAlbums) },
                    onEnqueueClick = { viewModel.enqueueAlbums(selectedAlbums, context) },
                    onUnselectAllClick = { viewModel.unselectAllAlbums() },
                    onSelectAllClick = { viewModel.selectAlbums(albumPojos.map { it.album }) },
                )

                val onEmpty: @Composable (Boolean, Boolean) -> Unit = { isImporting, isLoading ->
                    Log.i("LibraryScreen", "o/~ Running onEmpty ... o/~")
                    Text(
                        stringResource(
                            if (isImporting) R.string.importing_local_albums
                            else if (isLoading) R.string.loading_albums
                            else R.string.no_albums_found
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                ListActions(
                    initialSearchTerm = searchTerm,
                    sortParameter = sortParameter,
                    sortOrder = sortOrder,
                    sortParameters = AlbumSortParameter.withLabels(context),
                    sortDialogTitle = stringResource(R.string.album_order),
                    onSort = { param, order -> viewModel.setAlbumSorting(param, order) },
                    onSearch = { viewModel.setAlbumSearchTerm(it) },
                )

                when (displayType) {
                    DisplayType.LIST -> AlbumList(
                        pojos = albumPojos,
                        albumCallbacks = albumCallbacks,
                        albumSelectionCallbacks = albumSelectionCallbacks,
                        selectedAlbums = selectedAlbums,
                        listState = rememberLazyListState(),
                        onEmpty = { onEmpty(isImportingLocalMedia, isLoadingAlbums) },
                        albumDownloadTasks = albumDownloadTasks,
                    )
                    DisplayType.GRID -> AlbumGrid(
                        pojos = albumPojos,
                        albumCallbacks = albumCallbacks,
                        selectedAlbums = selectedAlbums,
                        albumSelectionCallbacks = albumSelectionCallbacks,
                        onEmpty = { onEmpty(isImportingLocalMedia, isLoadingAlbums) },
                        albumDownloadTasks = albumDownloadTasks,
                    )
                }
            }
            ListType.TRACKS -> {
                val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
                val latestSelectedTrackPojo by viewModel.latestSelectedTrackPojo.collectAsStateWithLifecycle(null)
                val searchTerm by viewModel.trackSearchTerm.collectAsStateWithLifecycle()
                val selectedTrackPojos by viewModel.selectedTrackPojos.collectAsStateWithLifecycle()
                val sortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
                val sortParameter by viewModel.trackSortParameter.collectAsStateWithLifecycle()
                val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
                val trackPojos = viewModel.pagingTrackPojos.collectAsLazyPagingItems()

                var latestSelectedIndex by rememberSaveable(selectedTrackPojos) { mutableStateOf<Int?>(null) }

                val trackCallbacks = { index: Int, pojo: TrackPojo ->
                    TrackCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onTrackClick = {
                            if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(pojo)
                            else viewModel.playTrackPojo(pojo)
                        },
                        onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                        onLongClick = {
                            viewModel.selectTrackPojos(
                                latestSelectedIndex?.let { index2 ->
                                    (min(index, index2)..max(index, index2)).mapNotNull { idx -> trackPojos[idx] }
                                } ?: listOf(pojo)
                            )
                        },
                        onEach = {
                            if (pojo.track.trackId == latestSelectedTrackPojo?.track?.trackId)
                                latestSelectedIndex = index
                        },
                    )
                }
                val trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackPojos.tracks()))
                    },
                    onPlayClick = { viewModel.playTrackPojos(selectedTrackPojos) },
                    onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTrackPojos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
                )
                val onEmpty: @Composable (Boolean, Boolean) -> Unit = { isImporting, isLoading ->
                    Text(
                        stringResource(
                            if (isImporting) R.string.importing_local_tracks
                            else if (isLoading) R.string.loading_tracks
                            else R.string.no_tracks_found
                        ),
                        modifier = Modifier.padding(10.dp),
                    )
                }

                ListActions(
                    initialSearchTerm = searchTerm,
                    sortParameter = sortParameter,
                    sortOrder = sortOrder,
                    sortParameters = TrackSortParameter.withLabels(context),
                    sortDialogTitle = stringResource(R.string.track_order),
                    onSort = { param, order -> viewModel.setTrackSorting(param, order) },
                    onSearch = { viewModel.setTrackSearchTerm(it) },
                )

                when (displayType) {
                    DisplayType.LIST -> TrackList(
                        trackPojos = trackPojos,
                        selectedTrackPojos = selectedTrackPojos,
                        viewModel = viewModel,
                        listState = trackListState,
                        trackCallbacks = trackCallbacks,
                        trackSelectionCallbacks = trackSelectionCallbacks,
                        trackDownloadTasks = trackDownloadTasks,
                        onEmpty = { onEmpty(isImportingLocalMedia, isLoadingTracks) },
                    )
                    DisplayType.GRID -> TrackGrid(
                        trackPojos = trackPojos,
                        viewModel = viewModel,
                        gridState = trackGridState,
                        trackCallbacks = trackCallbacks,
                        trackSelectionCallbacks = trackSelectionCallbacks,
                        selectedTrackPojos = selectedTrackPojos,
                        trackDownloadTasks = trackDownloadTasks,
                        onEmpty = { onEmpty(isImportingLocalMedia, isLoadingTracks) },
                    )
                }
            }
            ListType.ARTISTS -> {
                when (displayType) {
                    DisplayType.LIST -> ArtistList(
                        isImporting = isImportingLocalMedia,
                        onArtistClick = appCallbacks.onArtistClick,
                    )
                    DisplayType.GRID -> ArtistGrid(
                        isImporting = isImportingLocalMedia,
                        onArtistClick = appCallbacks.onArtistClick,
                    )
                }
            }
            ListType.PLAYLISTS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
                    val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsStateWithLifecycle()

                    PlaylistList(
                        playlists = playlists,
                        viewModel = viewModel,
                        onPlaylistClick = { appCallbacks.onPlaylistClick(it.playlistId) },
                        onPlaylistPlayClick = { viewModel.playPlaylist(it.playlistId) },
                        onEmpty = {
                            Text(
                                text = stringResource(if (isLoadingPlaylists) R.string.loading_playlists else R.string.no_playlists_found),
                                modifier = Modifier.padding(10.dp),
                            )
                        },
                    )

                    FloatingActionButton(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 10.dp),
                        onClick = appCallbacks.onCreatePlaylistClick,
                        shape = CircleShape,
                        content = { Icon(Icons.Sharp.Add, stringResource(R.string.add_playlist)) },
                    )
                }
            }
        }
    }
}
