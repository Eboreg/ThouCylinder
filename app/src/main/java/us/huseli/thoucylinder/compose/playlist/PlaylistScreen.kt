package us.huseli.thoucylinder.compose.playlist

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.track.TrackListReorderable
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.compose.utils.SelectionAction
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel

@Composable
fun PlaylistScreen(modifier: Modifier = Modifier, viewModel: PlaylistViewModel = hiltViewModel()) {
    val appCallbacks = LocalAppCallbacks.current
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val stateOrNull by viewModel.playlistState.collectAsStateWithLifecycle()
    val selectedTrackCount by viewModel.selectedTrackCount.collectAsStateWithLifecycle()
    val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()
    val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onMoveTrack(from.index, to.index) },
        onDragEnd = { from, to -> viewModel.onMoveTrackFinished(from, to) },
    )

    stateOrNull?.also { state ->
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
                        text = state.name.umlautify(),
                        style = FistopyTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).basicMarquee(Int.MAX_VALUE, initialDelayMillis = 1000),
                    )

                    PlaylistBottomSheetWithButton(
                        name = state.name,
                        thumbnailUris = state.thumbnailUris,
                        onPlayClick = { viewModel.playPlaylist() },
                        onExportClick = { dialogCallbacks.onExportPlaylistClick(state.id) },
                        onDeleteClick = {
                            viewModel.deletePlaylist(
                                onGotoPlaylistClick = { appCallbacks.onGotoPlaylistClick(state.id) },
                            )
                        },
                        onRename = { viewModel.renamePlaylist(it) },
                    )
                }
            }

            TrackListReorderable(
                states = { trackUiStates },
                getDownloadStateFlow = remember { { viewModel.getTrackDownloadUiStateFlow(it) } },
                onClick = remember { { viewModel.onTrackClick(it) } },
                onLongClick = remember { { viewModel.onTrackLongClick(it.id) } },
                selectedTrackCount = { selectedTrackCount },
                trackSelectionCallbacks = viewModel.getTrackSelectionCallbacks(dialogCallbacks),
                reorderableState = reorderableState,
                isLoading = isLoadingTracks,
                showAlbum = false,
                showArtist = true,
                extraSelectionActions = persistentListOf(
                    SelectionAction(
                        onClick = { viewModel.removeSelectedPlaylistTracks() },
                        icon = Icons.Sharp.Delete,
                        description = R.string.remove,
                    )
                ),
            )
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
