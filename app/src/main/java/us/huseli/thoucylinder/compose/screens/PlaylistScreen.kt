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
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.views.PlaylistTrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
) {
    val colors = LocalBasicColors.current
    val context = LocalContext.current

    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle(null)
    val playlistOrNull by viewModel.playlist.collectAsStateWithLifecycle(null)
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val trackDownloadStates by viewModel.trackDownloadStates.collectAsStateWithLifecycle()
    val trackCombos: List<PlaylistTrackCombo> by viewModel.trackCombos.collectAsStateWithLifecycle()

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )

    var latestSelectedTrackIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }

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
                selectedTrackIds = selectedTrackIds,
                onEmpty = { Text(stringResource(R.string.this_playlist_is_empty), modifier = Modifier.padding(10.dp)) },
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks, context),
                extraTrackSelectionButtons = {
                    SmallOutlinedButton(
                        onClick = { viewModel.removeTrackCombos(selectedTrackIds) },
                        content = { Icon(Icons.Sharp.Delete, stringResource(R.string.remove)) },
                    )
                },
            ) {
                itemsIndexed(trackCombos, key = { _, combo -> combo.id }) { index, combo ->
                    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(combo.track.image, combo.album?.albumArt) {
                        thumbnail = viewModel.getTrackComboThumbnail(combo)
                        viewModel.ensureTrackMetadata(combo.track)
                    }

                    ReorderableItem(reorderableState = reorderableState, key = combo.id) { isDragging ->
                        val isSelected = selectedTrackIds.contains(combo.id)
                        val containerColor =
                            if (isDragging) MaterialTheme.colorScheme.tertiaryContainer
                            else null

                        TrackListRow(
                            combo = combo,
                            showArtist = true,
                            showAlbum = false,
                            thumbnail = { thumbnail },
                            isSelected = isSelected,
                            containerColor = containerColor,
                            reorderableState = reorderableState,
                            downloadState = trackDownloadStates.find { it.trackId == combo.track.trackId },
                            callbacks = TrackCallbacks(
                                state = combo.getViewState(),
                                appCallbacks = appCallbacks,
                                onTrackClick = {
                                    if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(combo.id)
                                    else if (combo.track.isPlayable) viewModel.playPlaylist(startAt = combo)
                                },
                                onEnqueueClick = if (combo.track.isPlayable) {
                                    { viewModel.enqueueTrackCombo(combo, context) }
                                } else null,
                                onLongClick = {
                                    viewModel.selectTracksFromLastSelected(
                                        to = combo.id,
                                        allTrackIds = trackCombos.map { it.id },
                                    )
                                },
                                onEach = {
                                    if (combo.track.trackId == latestSelectedTrackId) latestSelectedTrackIndex = index
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}
