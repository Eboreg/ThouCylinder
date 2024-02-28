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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.track.SelectedTracksButtons
import us.huseli.thoucylinder.compose.track.TrackListRow
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.QueueViewModel

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val context = LocalContext.current
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle(emptyList())
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val playerCurrentCombo by viewModel.currentCombo.collectAsStateWithLifecycle(null)

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )

    Column(modifier = modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            trackCount = selectedTrackIds.size,
            callbacks = viewModel.getTrackSelectionCallbacks(appCallbacks, context),
            extraButtons = {
                SmallOutlinedButton(
                    onClick = { viewModel.removeSelectedTracksFromQueue() },
                    text = stringResource(R.string.remove),
                )
            },
        )

        if (queue.isEmpty()) {
            Text(text = stringResource(R.string.the_queue_is_empty), modifier = Modifier.padding(10.dp))
        }

        ListWithNumericBar(
            listState = reorderableState.listState,
            listSize = queue.size,
            itemHeight = 55.dp,
            minItems = 50,
        ) {
            LazyColumn(
                modifier = Modifier.reorderable(reorderableState),
                state = reorderableState.listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                itemsIndexed(queue, key = { _, combo -> combo.queueTrackId }) { comboIdx, combo ->
                    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(combo.track.image, combo.album?.albumArt) {
                        thumbnail.value = viewModel.getTrackThumbnail(combo, context)
                        viewModel.ensureTrackMetadata(combo.track)
                    }

                    ReorderableItem(reorderableState = reorderableState, key = combo.queueTrackId) { isDragging ->
                        val isSelected = selectedTrackIds.contains(combo.queueTrackId)
                        val containerColor =
                            if (isDragging) MaterialTheme.colorScheme.tertiaryContainer
                            else if (!isSelected && playerCurrentCombo == combo)
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            else null

                        TrackListRow(
                            combo = combo,
                            showArtist = true,
                            thumbnail = thumbnail.value,
                            isSelected = isSelected,
                            callbacks = TrackCallbacks(
                                combo = combo,
                                appCallbacks = appCallbacks,
                                context = context,
                                onTrackClick = {
                                    if (selectedTrackIds.isNotEmpty()) viewModel.toggleSelected(combo)
                                    else viewModel.skipTo(comboIdx)
                                },
                                onLongClick = {
                                    viewModel.selectTracksFromLastSelected(
                                        to = combo.queueTrackId,
                                        allTrackIds = queue.map { it.queueTrackId },
                                    )
                                },
                            ),
                            extraContextMenuItems = {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.remove_from_queue)) },
                                    leadingIcon = { Icon(Icons.Sharp.Delete, null) },
                                    onClick = { viewModel.removeFromQueue(combo.queueTrackId) },
                                )
                            },
                            downloadTask = trackDownloadTasks.find { it.trackCombo.track.trackId == combo.track.trackId },
                            containerColor = containerColor,
                            reorderableState = reorderableState,
                        )
                    }
                }
            }
        }
    }
}
