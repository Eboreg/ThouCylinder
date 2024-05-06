package us.huseli.thoucylinder.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.uistates.PlaylistDialogUiState
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AppViewModel

@Composable
fun PlaylistDialog(
    trackIds: ImmutableList<String>,
    onPlaylistClick: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.playlistPojos.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(PlaylistDialogUiState(playlists = playlists, trackIds = trackIds))
    }
    val displayAddedToPlaylistMessage: (String, Int) -> Unit = { playlistId, trackCount ->
        SnackbarEngine.addInfo(
            message = context.resources
                .getQuantityString(R.plurals.x_tracks_added_to_playlist, trackCount, trackCount)
                .umlautify(),
            actionLabel = context.getString(R.string.go_to_playlist).umlautify(),
            onActionPerformed = { onPlaylistClick(playlistId) },
        )
    }

    PlaylistDialog(
        state = state,
        onSelect = { playlistId ->
            scope.launch {
                val duplicateCount = viewModel.getDuplicatePlaylistTrackCount(playlistId, trackIds)

                if (duplicateCount > 0) {
                    state = state.copy(duplicateCount = duplicateCount, selectedPlaylistId = playlistId)
                } else {
                    val added = viewModel.addTracksToPlaylist(
                        playlistId = playlistId,
                        trackIds = trackIds,
                    )
                    displayAddedToPlaylistMessage(playlistId, added)
                    onClose()
                }
            }
        },
        onCreateNewClick = { state = state.copy(createPlaylist = true) },
        onSaveNewClick = { name ->
            val playlist = Playlist(name = name)

            viewModel.createPlaylist(playlist, trackIds)
            displayAddedToPlaylistMessage(playlist.playlistId, trackIds.size)
            onClose()
        },
        onCancel = { onClose() },
        onAddDuplicatesClick = { includeDuplicates ->
            val playlistId = state.selectedPlaylistId

            scope.launch {
                if (playlistId != null) {
                    val added = viewModel.addTracksToPlaylist(
                        playlistId = playlistId,
                        trackIds = trackIds,
                        includeDuplicates = includeDuplicates,
                    )
                    displayAddedToPlaylistMessage(playlistId, added)
                }
                onClose()
            }
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDialog(
    state: PlaylistDialogUiState,
    onSelect: (String) -> Unit,
    onCreateNewClick: () -> Unit,
    onSaveNewClick: (String) -> Unit,
    onCancel: () -> Unit,
    onAddDuplicatesClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newPlaylistName by rememberSaveable { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onCancel, modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (state.duplicateCount > 0) {
                    Text(
                        pluralStringResource(
                            id = R.plurals.x_selected_tracks_already_in_playlist,
                            count = state.duplicateCount,
                            state.duplicateCount,
                        )
                    )
                    Text(stringResource(R.string.what_do_you_want_to_do))
                } else if (state.createPlaylist) {
                    Text(stringResource(R.string.add_playlist), style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    )
                } else {
                    Text(stringResource(R.string.add_to_playlist), style = MaterialTheme.typography.headlineSmall)

                    ItemList(
                        things = state.playlists,
                        cardHeight = 50.dp,
                        onClick = { _, playlist -> onSelect(playlist.playlistId) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        key = { _, playlist -> playlist.playlistId },
                    ) { _, playlist ->
                        Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = playlist.name.umlautify(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = " • " + pluralStringResource(
                                        R.plurals.x_tracks,
                                        playlist.trackCount,
                                        playlist.trackCount,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    if (state.playlists.isEmpty()) Text(text = stringResource(R.string.no_playlists_found))
                }

                Row(modifier = Modifier.align(Alignment.End).padding(top = 20.dp)) {
                    if (state.duplicateCount > 0) {
                        CancelButton(onClick = { onAddDuplicatesClick(false) }) { Text(stringResource(R.string.skip)) }
                        SaveButton(onClick = { onAddDuplicatesClick(true) }) { Text(stringResource(R.string.add_anyway)) }
                    } else {
                        CancelButton(onClick = onCancel)
                    }
                    if (state.createPlaylist) {
                        SaveButton(
                            onClick = { onSaveNewClick(newPlaylistName) },
                            enabled = newPlaylistName.isNotBlank(),
                        )
                    } else {
                        SaveButton(onClick = onCreateNewClick) { Text(stringResource(R.string.create_new_playlist)) }
                    }
                }
            }
        }
    }
}
