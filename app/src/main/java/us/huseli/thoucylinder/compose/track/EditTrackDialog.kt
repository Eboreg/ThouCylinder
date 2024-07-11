package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AutocompleteTextField
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.EditTrackViewModel

@Composable
fun EditTrackDialog(
    state: TrackUiState,
    modifier: Modifier = Modifier,
    viewModel: EditTrackViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(state.title) }
    var artistNames by rememberSaveable {
        mutableStateOf(state.artists.map { it.name }.takeIf { it.isNotEmpty() } ?: listOf(""))
    }
    var albumPosition by rememberSaveable { mutableStateOf(state.albumPosition?.toString() ?: "") }
    var discNumber by rememberSaveable { mutableStateOf(state.discNumber?.toString() ?: "") }
    var year by rememberSaveable { mutableStateOf(state.year?.toString() ?: "") }

    AlertDialog(
        modifier = modifier.padding(20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.small,
        dismissButton = { CancelButton(onClick = onClose) },
        confirmButton = {
            SaveButton(
                onClick = {
                    viewModel.updateTrack(
                        trackId = state.trackId,
                        title = title,
                        year = year.toIntOrNull(),
                        albumPosition = albumPosition.toIntOrNull(),
                        discNumber = discNumber.toIntOrNull(),
                        artistNames = artistNames,
                    )
                    onClose()
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        artistNames.forEachIndexed { index, artistName ->
                            val onTextChange: (String) -> Unit = {
                                artistNames = artistNames.toMutableList().apply { set(index, it) }
                            }

                            AutocompleteTextField(
                                initial = artistName,
                                getSuggestions = { viewModel.getArtistNameSuggestions(it) },
                                onSelect = onTextChange,
                                onTextChange = onTextChange,
                            ) { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) }
                        }
                    }
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.year)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.3f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = discNumber,
                        onValueChange = { discNumber = it },
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.disc_number)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.5f),
                    )
                    OutlinedTextField(
                        value = albumPosition,
                        onValueChange = { albumPosition = it },
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.track_number)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.5f),
                    )
                }
            }
        }
    )
}
