package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import us.huseli.retaintheme.compose.SmallOutlinedButton
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    appCallbacks: AppCallbacks,
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle(
        PlaylistPojo(playlistId = viewModel.playlistId)
    )
    val trackPojos: LazyPagingItems<PlaylistTrackPojo> = viewModel.trackPojos.collectAsLazyPagingItems()
    val selectedTrackPojos by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    val colors = LocalBasicColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = appCallbacks.onBackClick,
                    content = { Icon(Icons.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                )
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { appCallbacks.onDeletePlaylistClick(playlist) },
                    content = { Icon(Icons.Sharp.Delete, stringResource(R.string.remove_playlist), tint = colors.Red) },
                )
                IconButton(
                    onClick = { viewModel.playPlaylist() },
                    content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
                )
            }
        }

        TrackList(
            trackPojos = trackPojos,
            viewModel = viewModel,
            listState = listState,
            selectedTrackPojos = selectedTrackPojos,
            trackDownloadTasks = trackDownloadTasks,
            onEmpty = { Text(stringResource(R.string.this_playlist_is_empty), modifier = Modifier.padding(10.dp)) },
            trackCallbacks = { pojo: PlaylistTrackPojo ->
                TrackCallbacks.fromAppCallbacks(
                    pojo = pojo,
                    appCallbacks = appCallbacks,
                    onTrackClick = {
                        if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(pojo)
                        else viewModel.playPlaylist(startAt = pojo)
                    },
                    onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                    onLongClick = { viewModel.selectTrackPojosFromLastSelected(to = pojo) },
                )
            },
            trackSelectionCallbacks = TrackSelectionCallbacks(
                onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(trackPojos = selectedTrackPojos)) },
                onPlayClick = { viewModel.playTrackPojos(selectedTrackPojos) },
                onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTrackPojos, context) },
                onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
            ),
            extraTrackSelectionButtons = {
                SmallOutlinedButton(
                    onClick = { viewModel.removeTrackPojos(selectedTrackPojos) },
                    text = stringResource(R.string.add_to_playlist),
                )
            }
        )
    }
}
