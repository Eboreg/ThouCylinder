package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.track.TrackListRow
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.tracks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val colors = LocalBasicColors.current
    val context = LocalContext.current

    val latestSelectedTrackCombo by viewModel.latestSelectedTrackCombo.collectAsStateWithLifecycle(null)
    val playlistOrNull by viewModel.playlist.collectAsStateWithLifecycle(null)
    val selectedTrackCombos by viewModel.selectedPlaylistTrackCombos.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val trackCombos by viewModel.trackCombos.collectAsStateWithLifecycle()

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )

    var latestSelectedTrackIndex by rememberSaveable(selectedTrackCombos) { mutableStateOf<Int?>(null) }

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
                        content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                    )
                    Text(
                        text = playlist.name.umlautify(),
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
                listState = reorderableState.listState,
                modifier = Modifier.reorderable(reorderableState),
                itemCount = trackCombos.size,
                selectedTrackCombos = selectedTrackCombos,
                onEmpty = { Text(stringResource(R.string.this_playlist_is_empty), modifier = Modifier.padding(10.dp)) },
                trackSelectionCallbacks = TrackSelectionCallbacks(
                    onAddToPlaylistClick = {
                        appCallbacks.onAddToPlaylistClick(Selection(tracks = selectedTrackCombos.tracks()))
                    },
                    onPlayClick = { viewModel.playTrackCombos(selectedTrackCombos) },
                    onEnqueueClick = { viewModel.enqueueTrackCombos(selectedTrackCombos, context) },
                    onUnselectAllClick = { viewModel.unselectAllTrackCombos() },
                ),
                extraTrackSelectionButtons = {
                    SmallOutlinedButton(
                        onClick = { viewModel.removeTrackCombos(selectedTrackCombos) },
                        text = stringResource(R.string.remove),
                    )
                },
            ) {
                itemsIndexed(trackCombos, key = { _, combo -> combo.id }) { index, combo ->
                    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(combo.track.trackId) {
                        thumbnail.value = viewModel.getTrackThumbnail(track = combo.track, album = combo.album)
                        viewModel.ensureTrackMetadata(combo.track)
                    }

                    ReorderableItem(reorderableState = reorderableState, key = combo.id) { isDragging ->
                        val isSelected = selectedTrackCombos.contains(combo)
                        val containerColor =
                            if (isDragging) MaterialTheme.colorScheme.tertiaryContainer
                            else null

                        TrackListRow(
                            combo = combo,
                            showArtist = true,
                            thumbnail = thumbnail.value,
                            isSelected = isSelected,
                            containerColor = containerColor,
                            reorderableState = reorderableState,
                            callbacks = TrackCallbacks(
                                combo = combo,
                                appCallbacks = appCallbacks,
                                context = context,
                                onTrackClick = {
                                    if (selectedTrackCombos.isNotEmpty()) viewModel.toggleSelected(combo)
                                    else viewModel.playPlaylist(startAt = combo)
                                },
                                onEnqueueClick = { viewModel.enqueueTrackCombo(combo, context) },
                                onLongClick = {
                                    viewModel.selectPlaylistTrackCombos(
                                        latestSelectedTrackIndex?.let { index2 ->
                                            (min(index, index2)..max(index, index2))
                                                .mapNotNull { idx -> trackCombos[idx] }
                                        } ?: listOf(combo)
                                    )
                                },
                                onEach = {
                                    if (combo.track.trackId == latestSelectedTrackCombo?.track?.trackId)
                                        latestSelectedTrackIndex = index
                                },
                            ),
                            downloadTask = trackDownloadTasks.find { it.track.trackId == combo.track.trackId },
                        )
                    }
                }
            }
        }
    }
}
