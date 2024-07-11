package us.huseli.thoucylinder.compose.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ItemListCard
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.dataclasses.playlist.Playlist
import us.huseli.thoucylinder.dataclasses.playlist.PlaylistDialogUiState
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.PlaylistListViewModel

@Composable
fun AddTracksToPlaylistDialog(
    trackIds: ImmutableList<String>,
    onPlaylistClick: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: PlaylistListViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiStates by viewModel.playlistUiStates.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var state by remember(uiStates, isLoading) {
        mutableStateOf(PlaylistDialogUiState(uiStates = uiStates, trackIds = trackIds, isLoading = isLoading))
    }

    AddTracksToPlaylistDialog(
        state = state,
        onSelect = { playlistId ->
            scope.launch {
                val duplicateCount = viewModel.getDuplicatePlaylistTrackCount(playlistId, trackIds)

                if (duplicateCount > 0) {
                    state = state.copy(duplicateCount = duplicateCount, selectedPlaylistId = playlistId)
                } else {
                    viewModel.addTracksToPlaylist(
                        playlistId = playlistId,
                        trackIds = trackIds,
                        onPlaylistClick = { onPlaylistClick(playlistId) },
                    )
                    onClose()
                }
            }
        },
        onCreateNewClick = { state = state.copy(createPlaylist = true) },
        onSaveNewClick = { name ->
            val playlist = Playlist(name = name)

            viewModel.createPlaylist(
                playlist = playlist,
                addTracks = trackIds,
                onPlaylistClick = { onPlaylistClick(playlist.playlistId) },
            )
            onClose()
        },
        onCancel = { onClose() },
        onAddDuplicatesClick = { includeDuplicates ->
            val playlistId = state.selectedPlaylistId

            if (playlistId != null) {
                viewModel.addTracksToPlaylist(
                    playlistId = playlistId,
                    trackIds = trackIds,
                    includeDuplicates = includeDuplicates,
                    onPlaylistClick = { onPlaylistClick(playlistId) },
                )
            }
            onClose()
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTracksToPlaylistDialog(
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
                    Text(stringResource(R.string.add_playlist), style = FistopyTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    )
                } else {
                    Text(stringResource(R.string.add_to_playlist), style = FistopyTheme.typography.headlineSmall)

                    ItemList(
                        things = { state.uiStates },
                        key = { it.id },
                        isLoading = state.isLoading,
                        onEmpty = { Text(text = stringResource(R.string.no_playlists_found)) },
                        contentType = "PlaylistUiState",
                    ) { uiState ->
                        ItemListCard(
                            height = 50.dp,
                            onClick = { onSelect(uiState.id) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = uiState.name.umlautify(),
                                        style = FistopyTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = " • " + pluralStringResource(
                                            R.plurals.x_tracks,
                                            uiState.trackCount,
                                            uiState.trackCount,
                                        ),
                                        style = FistopyTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }

                    if (state.isLoading) CircularProgressIndicator()
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
