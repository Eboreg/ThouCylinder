package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
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
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.ArtistGrid
import us.huseli.thoucylinder.compose.ArtistList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.PlaylistList
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
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
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    val tracksPojos = viewModel.pagingTrackPojos.collectAsLazyPagingItems()
    val artistImages by viewModel.artistImages.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())
    val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
    val selectedTracks by viewModel.selectedTracks.collectAsStateWithLifecycle()
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
                val albumCallbacks = { pojo: AbstractAlbumPojo ->
                    AlbumCallbacks.fromAppCallbacks(
                        album = pojo.album,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playAlbum(pojo.album) },
                        onEnqueueClick = { viewModel.enqueueAlbum(pojo.album, context) },
                        onRemoveFromLibraryClick = { viewModel.removeAlbumFromLibrary(pojo.album) },
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

                when (displayType) {
                    DisplayType.LIST -> AlbumList(
                        pojos = albumPojos,
                        albumCallbacks = albumCallbacks,
                        albumSelectionCallbacks = albumSelectionCallbacks,
                        selectedAlbums = selectedAlbums,
                        listState = rememberLazyListState(),
                        onEmpty = {
                            Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                        },
                    )
                    DisplayType.GRID -> AlbumGrid(
                        pojos = albumPojos,
                        albumCallbacks = albumCallbacks,
                        selectedAlbums = selectedAlbums,
                        albumSelectionCallbacks = albumSelectionCallbacks,
                        onEmpty = {
                            Text(stringResource(R.string.no_albums_found), modifier = Modifier.padding(10.dp))
                        },
                    )
                }
            }
            ListType.TRACKS -> {
                val trackCallbacks = { pojo: TrackPojo ->
                    TrackCallbacks.fromAppCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        onTrackClick = {
                            if (selectedTracks.isNotEmpty()) viewModel.toggleSelected(pojo)
                            else viewModel.playTrackPojo(pojo)
                        },
                        onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                        onLongClick = { viewModel.selectTracksFromLastSelected(to = pojo) },
                    )
                }
                val trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTracks)) },
                    onPlayClick = { viewModel.playTrackPojos(selectedTracks) },
                    onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTracks, context) },
                    onUnselectAllClick = { viewModel.unselectAllTracks() },
                )

                when (displayType) {
                    DisplayType.LIST -> TrackList(
                        trackPojos = tracksPojos,
                        selectedTracks = selectedTracks,
                        viewModel = viewModel,
                        listState = trackListState,
                        trackCallbacks = trackCallbacks,
                        trackSelectionCallbacks = trackSelectionCallbacks,
                        onEmpty = {
                            Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                        },
                    )
                    DisplayType.GRID -> TrackGrid(
                        trackPojos = tracksPojos,
                        viewModel = viewModel,
                        gridState = trackGridState,
                        trackCallbacks = trackCallbacks,
                        trackSelectionCallbacks = trackSelectionCallbacks,
                        selectedTracks = selectedTracks,
                        onEmpty = {
                            Text(stringResource(R.string.no_tracks_found), modifier = Modifier.padding(10.dp))
                        },
                    )
                }
            }
            ListType.ARTISTS -> when (displayType) {
                DisplayType.LIST -> ArtistList(
                    artists = artistPojos,
                    images = artistImages,
                    onArtistClick = appCallbacks.onArtistClick,
                )
                DisplayType.GRID -> ArtistGrid(
                    artists = artistPojos,
                    images = artistImages,
                    onArtistClick = appCallbacks.onArtistClick,
                )
            }
            ListType.PLAYLISTS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    PlaylistList(
                        playlists = playlists,
                        viewModel = viewModel,
                        onPlaylistClick = { appCallbacks.onPlaylistClick(it.playlistId) },
                        onPlaylistPlayClick = { viewModel.playPlaylist(it.playlistId) },
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

        if (BuildConfig.DEBUG) {
            Row {
                TextButton(
                    onClick = { viewModel.deleteAll() },
                    content = { Text(text = "Delete all") }
                )
            }
        }
    }
}
