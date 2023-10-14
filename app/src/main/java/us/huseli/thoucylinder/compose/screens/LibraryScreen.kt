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
    onCreatePlaylistClick: () -> Unit,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    val tracksPojos = viewModel.pagingTrackPojos.collectAsLazyPagingItems()
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

        Column {
            when (listType) {
                ListType.ALBUMS -> when (displayType) {
                    DisplayType.LIST -> AlbumList(
                        pojos = albumPojos,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.album.albumId) },
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onArtistClick = onArtistClick,
                    )
                    DisplayType.GRID -> AlbumGrid(
                        albums = albumPojos,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.album.albumId) },
                    )
                }
                ListType.TRACKS -> when (displayType) {
                    DisplayType.LIST -> TrackList(
                        pojos = tracksPojos,
                        viewModel = viewModel,
                        listState = trackListState,
                        onDownloadClick = { viewModel.downloadTrack(it) },
                        onPlayClick = { viewModel.playTrack(it) },
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                    DisplayType.GRID -> TrackGrid(
                        pojos = tracksPojos,
                        viewModel = viewModel,
                        gridState = trackGridState,
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
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
                        viewModel = viewModel,
                        onPlaylistClick = { onPlaylistClick(it.playlistId) },
                        onPlaylistPlayClick = { viewModel.playPlaylist(it.playlistId) },
                        onCreatePlaylistClick = onCreatePlaylistClick,
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
