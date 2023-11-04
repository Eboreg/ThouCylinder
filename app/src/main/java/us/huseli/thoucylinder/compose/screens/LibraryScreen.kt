package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.BuildConfig
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
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
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

        Column {
            when (listType) {
                ListType.ALBUMS -> {
                    val albumCallbacks = { album: Album ->
                        AlbumCallbacks.fromAppCallbacks(
                            album = album,
                            appCallbacks = appCallbacks,
                            onPlayClick = { viewModel.playAlbum(album) },
                            onPlayNextClick = { viewModel.playAlbumNext(album, context) },
                            onRemoveFromLibraryClick = { viewModel.removeAlbumFromLibrary(album) },
                            onAlbumLongClick = { viewModel.selectAlbumsFromLastSelected(album) },
                            onAlbumClick = {
                                if (selectedAlbums.isNotEmpty()) viewModel.toggleSelected(album)
                                else appCallbacks.onAlbumClick(album.albumId)
                            },
                        )
                    }
                    val albumSelectionCallbacks = AlbumSelectionCallbacks(
                        albums = selectedAlbums,
                        appCallbacks = appCallbacks,
                        onPlayClick = { viewModel.playAlbums(selectedAlbums) },
                        onPlayNextClick = { viewModel.playAlbumsNext(selectedAlbums, context) },
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
                        )
                        DisplayType.GRID -> AlbumGrid(
                            albums = albumPojos,
                            albumCallbacks = albumCallbacks,
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
                            onPlayNextClick = { viewModel.playTrackPojoNext(pojo, context) },
                            onLongClick = { viewModel.selectTracksFromLastSelected(to = pojo) },
                        )
                    }
                    val trackSelectionCallbacks = TrackSelectionCallbacks(
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTracks)) },
                        onPlayClick = { viewModel.playTrackPojos(selectedTracks) },
                        onPlayNextClick = { viewModel.playTrackPojosNext(selectedTracks, context) },
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
                        )
                        DisplayType.GRID -> TrackGrid(
                            trackPojos = tracksPojos,
                            viewModel = viewModel,
                            gridState = trackGridState,
                            trackCallbacks = trackCallbacks,
                            trackSelectionCallbacks = trackSelectionCallbacks,
                            selectedTracks = selectedTracks,
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
                    PlaylistList(
                        playlists = playlists,
                        viewModel = viewModel,
                        onPlaylistClick = { appCallbacks.onPlaylistClick(it.playlistId) },
                        onPlaylistPlayClick = { viewModel.playPlaylist(it.playlistId) },
                        onCreatePlaylistClick = appCallbacks.onCreatePlaylistClick,
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
