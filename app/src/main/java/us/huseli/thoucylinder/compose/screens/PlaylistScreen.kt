package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
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
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    appCallbacks: AppCallbacks,
    trackCallbacks: TrackCallbacks,
) {
    val colors = LocalBasicColors.current

    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle()
    val playlistOrNull by viewModel.playlist.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val trackUiStates: List<TrackUiState> by viewModel.trackUiStates.collectAsStateWithLifecycle()

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )

    var latestSelectedTrackIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }
    var isRenameDialogOpen by rememberSaveable { mutableStateOf(false) }

    playlistOrNull?.also { playlist ->
        if (isRenameDialogOpen) {
            RenamePlaylistDialog(
                initialName = playlist.name,
                onSave = {
                    viewModel.renamePlaylist(it)
                    isRenameDialogOpen = false
                },
                onCancel = { isRenameDialogOpen = false },
            )
        }

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
                        modifier = Modifier.weight(1f).basicMarquee(Int.MAX_VALUE, initialDelayMillis = 1000),
                    )
                    IconButton(
                        onClick = { isRenameDialogOpen = true },
                        content = { Icon(Icons.Sharp.Edit, stringResource(R.string.edit)) },
                    )
                    IconButton(
                        onClick = {
                            appCallbacks.onDeletePlaylistClick(playlist.playlistId)
                            appCallbacks.onBackClick()
                        },
                        content = {
                            Icon(
                                imageVector = Icons.Sharp.Delete,
                                contentDescription = stringResource(R.string.remove_playlist),
                                tint = colors.Red,
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
                itemCount = trackUiStates.size,
                selectedTrackIds = selectedTrackIds,
                onEmpty = { Text(stringResource(R.string.this_playlist_is_empty), modifier = Modifier.padding(10.dp)) },
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(appCallbacks),
                extraTrackSelectionButtons = {
                    SmallOutlinedButton(
                        onClick = { viewModel.removeTrackCombos(selectedTrackIds) },
                        content = { Icon(Icons.Sharp.Delete, stringResource(R.string.remove)) },
                    )
                },
            ) {
                itemsIndexed(trackUiStates, key = { _, state -> state.id }) { index, state ->
                    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(state.trackThumbnailUri, state.albumThumbnailUri) {
                        thumbnail = viewModel.getTrackUiStateThumbnail(state)
                        viewModel.ensureTrackMetadata(state)
                    }

                    ReorderableItem(reorderableState = reorderableState, key = state.id) { isDragging ->
                        val isSelected = selectedTrackIds.contains(state.id)
                        val containerColor =
                            if (isDragging) MaterialTheme.colorScheme.tertiaryContainer
                            else null
                        val downloadState = state.downloadState.collectAsStateWithLifecycle()

                        TrackListRow(
                            state = state,
                            showArtist = true,
                            showAlbum = false,
                            thumbnail = { thumbnail },
                            isSelected = isSelected,
                            containerColor = containerColor,
                            reorderableState = reorderableState,
                            downloadState = downloadState,
                            callbacks = remember {
                                trackCallbacks.copy(
                                    onTrackClick = {
                                        if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(state.id)
                                        else if (state.isPlayable) viewModel.playPlaylist(startAtTrackId = state.trackId)
                                    },
                                    onLongClick = {
                                        viewModel.selectTracksFromLastSelected(
                                            to = state.id,
                                            allTrackIds = trackUiStates.map { it.id },
                                        )
                                    },
                                    onEach = {
                                        if (state.id == latestSelectedTrackId) {
                                            latestSelectedTrackIndex = index
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenamePlaylistDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = { SaveButton(onClick = { onSave(name) }, enabled = name.isNotEmpty()) },
        dismissButton = { CancelButton(onClick = onCancel) },
        title = { Text(stringResource(R.string.rename_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { OutlinedTextFieldLabel(stringResource(R.string.name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            )
        },
        shape = MaterialTheme.shapes.small,
    )
}
