package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onAlbumClick: (UUID) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle(emptyList())
    val tracks by viewModel.tracks.collectAsStateWithLifecycle(emptyList())
    val artistImages by viewModel.artistImages.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val artistTrackMap by viewModel.artistsWithTracks.collectAsStateWithLifecycle(emptyMap())

    Column(modifier = modifier) {
        ListSettings(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
        )

        when (displayType) {
            DisplayType.LIST -> {
                when (listType) {
                    ListType.ALBUMS -> AlbumList(
                        albums = albums,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.albumId) },
                    )
                    ListType.TRACKS -> TrackList(
                        tracks = tracks,
                        viewModel = viewModel,
                        onDownloadClick = { viewModel.downloadTrack(it) },
                        onPlayOrPauseClick = { viewModel.playOrPause(it) },
                        onGotoArtistClick = onArtistClick,
                        onGotoAlbumClick = onAlbumClick,
                    )
                    ListType.ARTISTS -> ArtistList(
                        viewModel = viewModel,
                        artistTrackMap = artistTrackMap,
                        images = artistImages,
                        albums = albums,
                        onArtistClick = onArtistClick,
                    )
                }
            }
            DisplayType.GRID -> {
                when (listType) {
                    ListType.ALBUMS -> AlbumGrid(
                        albums = albums,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.albumId) },
                    )
                    ListType.TRACKS -> TrackGrid(
                        tracks = tracks,
                        viewModel = viewModel,
                        onDownloadClick = { viewModel.downloadTrack(it) },
                        onPlayOrPauseClick = { viewModel.playOrPause(it) },
                        onGotoArtistClick = onArtistClick,
                        onGotoAlbumClick = onAlbumClick,
                    )
                    ListType.ARTISTS -> ArtistGrid(
                        viewModel = viewModel,
                        artistTrackMap = artistTrackMap,
                        images = artistImages,
                        albums = albums,
                        onArtistClick = onArtistClick,
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
