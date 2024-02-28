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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AutocompleteTextField
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.EditTrackViewModel

@Composable
fun EditTrackDialog(
    trackCombo: AbstractTrackCombo,
    modifier: Modifier = Modifier,
    viewModel: EditTrackViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val totalAreaSize by viewModel.totalAreaSize.collectAsStateWithLifecycle(DpSize.Zero)
    var dialogSize by remember { mutableStateOf(DpSize.Zero) }
    var title by rememberSaveable { mutableStateOf(trackCombo.track.title) }
    var artistNames by rememberSaveable {
        mutableStateOf(trackCombo.artists.map { it.name }.takeIf { it.isNotEmpty() } ?: listOf(""))
    }
    var albumPosition by rememberSaveable { mutableStateOf(trackCombo.track.albumPosition?.toString() ?: "") }
    var discNumber by rememberSaveable { mutableStateOf(trackCombo.track.discNumber?.toString() ?: "") }
    var year by rememberSaveable { mutableStateOf(trackCombo.track.year?.toString() ?: "") }

    AlertDialog(
        modifier = modifier
            .padding(20.dp)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                dialogSize = with(density) { DpSize(bounds.width.toDp(), bounds.height.toDp()) }
            },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.small,
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.updateTrackCombo(
                        combo = trackCombo,
                        title = title,
                        year = year.toIntOrNull(),
                        albumPosition = albumPosition.toIntOrNull(),
                        discNumber = discNumber.toIntOrNull(),
                        artistNames = artistNames,
                    )
                    onClose()
                },
                content = { Text(stringResource(R.string.save)) },
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
                        artistNames.fastForEachIndexed { index, artistName ->
                            val onTextChange: (String) -> Unit = {
                                artistNames = artistNames.toMutableList().apply { set(index, it) }
                            }

                            AutocompleteTextField(
                                initial = artistName,
                                getSuggestions = { viewModel.getArtistNameSuggestions(it) },
                                onSelect = onTextChange,
                                onTextChange = onTextChange,
                                totalAreaHeight = totalAreaSize.height,
                                rootOffsetY = (totalAreaSize.height - dialogSize.height) / 2,
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
