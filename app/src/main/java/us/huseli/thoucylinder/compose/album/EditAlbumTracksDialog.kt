package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.dataclasses.album.EditAlbumUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel

@Composable
fun EditAlbumTracksDialog(
    albumUiState: EditAlbumUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    val trackUiStates by viewModel.trackUiStates.collectAsStateWithLifecycle()
    val discCount = remember(trackUiStates) { trackUiStates.mapNotNull { it.discNumber }.maxOrNull() ?: 1 }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.edit_album_tracks)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = { CancelButton(onClick = onClose, text = stringResource(R.string.close)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trackUiStates, key = { it.id }) { state ->
                    val position =
                        if (discCount > 1 && state.discNumber != null && state.albumPosition != null)
                            "${state.discNumber}.${state.albumPosition}. "
                        else state.albumPosition?.let { "$it. " } ?: ""
                    val artistString =
                        if (state.artistString != null && state.artistString != albumUiState.artistString)
                            "${state.artistString} - "
                        else ""
                    val year = state.year?.let { " ($it)" } ?: ""

                    EditIndividualAlbumTrackSection(
                        uiState = state,
                        trackString = "$position$artistString${state.title}$year",
                        enabled = true,
                        getArtistNameSuggestions = { name -> viewModel.getArtistNameSuggestions(name) },
                        onSaveClick = { data ->
                            viewModel.updateTrack(
                                trackId = state.trackId,
                                title = data.title,
                                year = data.year,
                                artistNames = data.artistNames,
                            )
                        }
                    )
                }
            }
        }
    )
}
