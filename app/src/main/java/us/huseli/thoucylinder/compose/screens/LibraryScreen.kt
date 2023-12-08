package us.huseli.thoucylinder.compose.screens

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
import us.huseli.thoucylinder.Selection
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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

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
                val searchTerm by viewModel.albumSearchTerm.collectAsStateWithLifecycle()
                val sortParameter by viewModel.albumSortParameter.collectAsStateWithLifecycle()
                val sortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
                val albumDownloadTasks by viewModel.albumDownloadTasks.collectAsStateWithLifecycle()
                val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
                val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())
                val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsStateWithLifecycle()
                val albumCallbacks = { pojo: AbstractAlbumPojo ->
                    AlbumCallbacks.fromAppCallbacks(
                        album = pojo.album,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playAlbum(pojo.album) },
                        onEnqueueClick = { viewModel.enqueueAlbum(pojo.album, context) },
                        onAlbumLongClick = { viewModel.selectAlbumsFromLastSelected(pojo.album) },
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
                val searchTerm by viewModel.trackSearchTerm.collectAsStateWithLifecycle()
                val sortParameter by viewModel.trackSortParameter.collectAsStateWithLifecycle()
                val sortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
                val trackPojos = viewModel.pagingTrackPojos.collectAsLazyPagingItems()
                val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
                val selectedTrackPojos by viewModel.selectedTrackPojos.collectAsStateWithLifecycle()
                val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
                val trackCallbacks = { pojo: TrackPojo ->
                    TrackCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        onTrackClick = {
                            if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(pojo)
                            else viewModel.playTrackPojo(pojo)
                        },
                        onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                        onLongClick = { viewModel.selectTrackPojosFromLastSelected(to = pojo) },
                    )
                }
                val trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTrackPojos)) },
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
                val artistImages by viewModel.artistImages.collectAsStateWithLifecycle()
                val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())
                val isLoadingArtists by viewModel.isLoadingArtists.collectAsStateWithLifecycle()
                val onEmpty: @Composable (Boolean, Boolean) -> Unit = { isImporting, isLoading ->
                    Text(
                        stringResource(
                            if (isImporting) R.string.importing_local_artists
                            else if (isLoading) R.string.loading_artists
                            else R.string.no_artists_found
                        ),
                        modifier = Modifier.padding(10.dp),
                    )
                }

                when (displayType) {
                    DisplayType.LIST -> ArtistList(
                        artists = artistPojos,
                        images = artistImages,
                        onArtistClick = appCallbacks.onArtistClick,
                        onEmpty = { onEmpty(isImportingLocalMedia, isLoadingArtists) },
                    )
                    DisplayType.GRID -> ArtistGrid(
                        artists = artistPojos,
                        images = artistImages,
                        onArtistClick = appCallbacks.onArtistClick,
                        onEmpty = { onEmpty(isImportingLocalMedia, isLoadingArtists) },
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
