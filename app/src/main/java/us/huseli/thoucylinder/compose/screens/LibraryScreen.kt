package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import us.huseli.thoucylinder.viewmodels.LibraryViewModel
import java.util.UUID

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    trackListState: LazyListState = rememberLazyListState(),
    trackGridState: LazyGridState = rememberLazyGridState(),
    onAlbumClick: (UUID) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (UUID) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    val tracks = viewModel.pagingTracks.collectAsLazyPagingItems()
    val artistImages by viewModel.artistImages.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())
    val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())
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

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            when (listType) {
                ListType.ALBUMS -> when (displayType) {
                    DisplayType.LIST -> AlbumList(
                        albums = albumPojos,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.album.albumId) },
                    )
                    DisplayType.GRID -> AlbumGrid(
                        albums = albumPojos,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.album.albumId) },
                    )
                }
                ListType.TRACKS -> when (displayType) {
                    DisplayType.LIST -> TrackList(
                        tracks = tracks,
                        viewModel = viewModel,
                        listState = trackListState,
                        onDownloadClick = { viewModel.downloadTrack(it) },
                        onPlayClick = { viewModel.play(it) },
                        onGotoArtistClick = onArtistClick,
                        onGotoAlbumClick = onAlbumClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                    DisplayType.GRID -> TrackGrid(
                        tracks = tracks,
                        viewModel = viewModel,
                        gridState = trackGridState,
                        onGotoArtistClick = onArtistClick,
                        onGotoAlbumClick = onAlbumClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }
                ListType.ARTISTS -> when (displayType) {
                    DisplayType.LIST -> ArtistList(
                        viewModel = viewModel,
                        artists = artistPojos,
                        images = artistImages,
                        onArtistClick = onArtistClick,
                    )
                    DisplayType.GRID -> ArtistGrid(
                        viewModel = viewModel,
                        artists = artistPojos,
                        images = artistImages,
                        onArtistClick = onArtistClick,
                    )
                }
                ListType.PLAYLISTS -> {
                    PlaylistList(
                        playlists = playlists,
                        onPlaylistClick = { onPlaylistClick(it.playlistId) },
                        // TODO: Add a callback
                        onPlaylistPlayClick = { },
                        onAddPlaylist = { viewModel.addPlaylist(it) },
                    )
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Row {
                Button(
                    onClick = { viewModel.deleteAll() },
                    content = { Text(text = "Delete all") }
                )
            }
        }
    }
}
