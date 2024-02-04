package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.abstr.tracks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    appCallbacks: AppCallbacks,
) {
    val colors = LocalBasicColors.current
    val context = LocalContext.current

    val latestSelectedTrackPojo by viewModel.latestSelectedTrackPojo.collectAsStateWithLifecycle(null)
    val playlistOrNull by viewModel.playlist.collectAsStateWithLifecycle(null)
    val selectedTrackPojos by viewModel.selectedPlaylistTrackPojos.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackPojos: LazyPagingItems<PlaylistTrackPojo> = viewModel.trackPojos.collectAsLazyPagingItems()

    var latestSelectedTrackIndex by rememberSaveable(selectedTrackPojos) { mutableStateOf<Int?>(null) }

    playlistOrNull?.also { playlist ->
        Column(modifier = modifier.fillMaxWidth()) {
            Surface(
                color = BottomAppBarDefaults.containerColor,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 5.dp),
                ) {
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
                        onClick = {
                            appCallbacks.onDeletePlaylistClick(playlist)
                            appCallbacks.onBackClick()
                        },
                        content = {
                            Icon(
                                Icons.Sharp.Delete,
                                stringResource(R.string.remove_playlist),
                                tint = colors.Red
                            )
                        },
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
                trackCallbacks = { index: Int, pojo: PlaylistTrackPojo ->
                    TrackCallbacks(
                        pojo = pojo,
                        appCallbacks = appCallbacks,
                        context = context,
                        onTrackClick = {
                            if (selectedTrackPojos.isNotEmpty()) viewModel.toggleSelected(pojo)
                            else viewModel.playPlaylist(startAt = pojo)
                        },
                        onEnqueueClick = { viewModel.enqueueTrackPojo(pojo, context) },
                        onLongClick = {
                            viewModel.selectPlaylistTrackPojos(
                                latestSelectedTrackIndex?.let { index2 ->
                                    (min(index, index2)..max(index, index2)).mapNotNull { idx -> trackPojos[idx] }
                                } ?: listOf(pojo)
                            )
                        },
                        onEach = {
                            if (pojo.track.trackId == latestSelectedTrackPojo?.track?.trackId)
                                latestSelectedTrackIndex = index
                        },
                    )
                },
                trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackPojos.tracks()))
                    },
                    onPlayClick = { viewModel.playTrackPojos(selectedTrackPojos) },
                    onEnqueueClick = { viewModel.enqueueTrackPojos(selectedTrackPojos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackPojos() },
                ),
                extraTrackSelectionButtons = {
                    SmallOutlinedButton(
                        onClick = { viewModel.removeTrackPojos(selectedTrackPojos) },
                        text = stringResource(R.string.remove),
                    )
                }
            )
        }
    }
}
