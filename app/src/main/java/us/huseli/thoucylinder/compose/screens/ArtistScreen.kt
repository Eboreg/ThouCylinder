package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettings
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.viewmodels.ArtistViewModel
import java.util.UUID

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAlbumClick: (UUID) -> Unit,
) {
    val artist = viewModel.artist
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) }
            )
            Text(text = artist, style = MaterialTheme.typography.headlineSmall)
        }

        ListSettings(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            excludeListTypes = listOf(ListType.ARTISTS),
        )

        when (listType) {
            ListType.ALBUMS -> when (displayType) {
                DisplayType.LIST -> AlbumList(
                    albums = albumPojos,
                    viewModel = viewModel,
                    showArtist = false,
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
                    onDownloadClick = { viewModel.downloadTrack(it) },
                    onPlayOrPauseClick = { viewModel.playOrPause(it) },
                    onGotoAlbumClick = onAlbumClick,
                    showArtist = false,
                )
                DisplayType.GRID -> TrackGrid(
                    tracks = tracks,
                    viewModel = viewModel,
                    onDownloadClick = { viewModel.downloadTrack(it) },
                    onPlayOrPauseClick = { viewModel.playOrPause(it) },
                    onGotoAlbumClick = onAlbumClick,
                    showArtist = false,
                )
            }
            ListType.ARTISTS -> {}
        }
    }
}
