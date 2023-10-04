package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.ArtistGrid
import us.huseli.thoucylinder.compose.ArtistList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettings
import us.huseli.thoucylinder.compose.ListType
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
) {
    val tracks = viewModel.pagingTracks.collectAsLazyPagingItems()
    val artistImages by viewModel.artistImages.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())
    val artistPojos by viewModel.artistPojos.collectAsStateWithLifecycle(emptyList())

    Column(modifier = modifier) {
        ListSettings(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
        )

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
                    onPlayOrPauseClick = { viewModel.playOrPause(it) },
                    onGotoArtistClick = onArtistClick,
                    onGotoAlbumClick = onAlbumClick,
                )
                DisplayType.GRID -> TrackGrid(
                    tracks = tracks,
                    viewModel = viewModel,
                    gridState = trackGridState,
                    onDownloadClick = { viewModel.downloadTrack(it) },
                    onPlayOrPauseClick = { viewModel.playOrPause(it) },
                    onGotoArtistClick = onArtistClick,
                    onGotoAlbumClick = onAlbumClick,
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
