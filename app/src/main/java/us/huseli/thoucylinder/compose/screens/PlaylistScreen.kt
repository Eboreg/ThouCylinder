package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import us.huseli.thoucylinder.compose.TrackList
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel
import java.util.UUID

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onAlbumClick: (UUID) -> Unit,
    onArtistClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle(
        PlaylistPojo(playlistId = viewModel.playlistId)
    )
    val tracks: LazyPagingItems<TrackPojo> = viewModel.tracks.collectAsLazyPagingItems()

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
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (tracks.itemCount == 0) {
            Text(text = stringResource(R.string.this_playlist_is_empty), modifier = Modifier.padding(10.dp))
        }

        TrackList(
            pojos = tracks,
            viewModel = viewModel,
            modifier = Modifier.padding(horizontal = 10.dp),
            onDownloadClick = { viewModel.downloadTrack(it) },
            onPlayClick = { viewModel.playTrack(it) },
            onAddToPlaylistClick = { onAddToPlaylistClick(it) },
            listState = listState,
            onArtistClick = onArtistClick,
            onAlbumClick = onAlbumClick,
        )
    }
}
