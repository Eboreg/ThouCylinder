package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.AlbumGrid
import us.huseli.thoucylinder.compose.AlbumList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.TrackGrid
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.ArtistViewModel
import java.util.UUID

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAlbumClick: (UUID) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
) {
    val artist = viewModel.artist
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val listType by viewModel.listType.collectAsStateWithLifecycle()
    val tracks: LazyPagingItems<TrackPojo> = viewModel.tracks.collectAsLazyPagingItems()
    val albumPojos by viewModel.albumPojos.collectAsStateWithLifecycle(emptyList())

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) }
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        ListSettingsRow(
            displayType = displayType,
            listType = listType,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            excludeListTypes = listOf(ListType.ARTISTS, ListType.PLAYLISTS),
        )

        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            when (listType) {
                ListType.ALBUMS -> when (displayType) {
                    DisplayType.LIST -> AlbumList(
                        pojos = albumPojos,
                        viewModel = viewModel,
                        showArtist = false,
                        onAlbumClick = { onAlbumClick(it.album.albumId) },
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                    DisplayType.GRID -> AlbumGrid(
                        albums = albumPojos,
                        viewModel = viewModel,
                        onAlbumClick = { onAlbumClick(it.album.albumId) },
                    )
                }
                ListType.TRACKS -> when (displayType) {
                    DisplayType.LIST -> TrackList(
                        pojos = tracks,
                        viewModel = viewModel,
                        onDownloadClick = { viewModel.downloadTrack(it) },
                        onPlayClick = { viewModel.playTrack(it) },
                        onAlbumClick = onAlbumClick,
                        showArtist = false,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                    DisplayType.GRID -> TrackGrid(
                        pojos = tracks,
                        viewModel = viewModel,
                        onAlbumClick = onAlbumClick,
                        showArtist = false,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }
                ListType.ARTISTS -> {}
                ListType.PLAYLISTS -> {}
            }
        }
    }
}
