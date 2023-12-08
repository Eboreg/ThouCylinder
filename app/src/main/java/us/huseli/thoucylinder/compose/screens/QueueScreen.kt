package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.retaintheme.compose.SmallOutlinedButton
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.track.TrackListRow
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.viewmodels.QueueViewModel

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current
    val selectedTracks by viewModel.selectedQueueTracks.collectAsStateWithLifecycle(emptyList())
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val playerCurrentPojo by viewModel.playerCurrentPojo.collectAsStateWithLifecycle(null)

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )

    Column(modifier = modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            trackCount = selectedTracks.size,
            callbacks = TrackSelectionCallbacks(
                onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(queueTracks = selectedTracks)) },
                onPlayClick = { viewModel.playQueueTracks(selectedTracks) },
                onEnqueueClick = { viewModel.enqueueQueueTracks(selectedTracks, context) },
                onUnselectAllClick = { viewModel.unselectAllQueueTracks() },
            ),
            extraButtons = {
                SmallOutlinedButton(
                    onClick = { viewModel.removeFromQueue(selectedTracks) },
                    text = stringResource(R.string.remove),
                )
            },
        )

        if (queue.isEmpty()) {
            Text(text = stringResource(R.string.the_queue_is_empty), modifier = Modifier.padding(10.dp))
        }

        ListWithNumericBar(listState = reorderableState.listState, listSize = queue.size) {
            LazyColumn(
                modifier = Modifier.reorderable(reorderableState),
                state = reorderableState.listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                itemsIndexed(queue, key = { _, pojo -> pojo.queueTrackId }) { pojoIdx, pojo ->
                    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(pojo.track.trackId) {
                        thumbnail.value = viewModel.getTrackThumbnail(pojo.track, pojo.album, context)
                        viewModel.ensureTrackMetadata(pojo.track, commit = true)
                    }

                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                        ReorderableItem(reorderableState = reorderableState, key = pojo.queueTrackId) { isDragging ->
                            val isSelected = selectedTracks.contains(pojo)
                            val containerColor =
                                if (isDragging) MaterialTheme.colorScheme.tertiaryContainer
                                else if (!isSelected && playerCurrentPojo == pojo)
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                else null

                            TrackListRow(
                                title = pojo.track.title,
                                isDownloadable = pojo.track.isDownloadable,
                                downloadTask = trackDownloadTasks.find { it.track.trackId == pojo.track.trackId },
                                thumbnail = thumbnail.value,
                                containerColor = containerColor,
                                reorderableState = reorderableState,
                                artist = pojo.artist,
                                duration = pojo.track.metadata?.duration,
                                callbacks = TrackCallbacks.fromAppCallbacks(
                                    pojo = pojo,
                                    appCallbacks = appCallbacks,
                                    onAlbumClick = pojo.album?.albumId?.let { { appCallbacks.onAlbumClick(it) } },
                                    onArtistClick = pojo.artist?.let { { appCallbacks.onArtistClick(it) } },
                                    onTrackClick = {
                                        if (selectedTracks.isNotEmpty()) viewModel.toggleSelected(pojo)
                                        else viewModel.skipTo(pojoIdx)
                                    },
                                    onLongClick = { viewModel.selectQueueTracksFromLastSelected(to = pojo) },
                                ),
                                isSelected = selectedTracks.contains(pojo),
                                extraContextMenuItems = {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(R.string.remove_from_queue)) },
                                        leadingIcon = { Icon(Icons.Sharp.Delete, null) },
                                        onClick = { viewModel.removeFromQueue(pojo) },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
