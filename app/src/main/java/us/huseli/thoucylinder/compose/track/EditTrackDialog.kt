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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Track

@Composable
fun EditTrackDialog(
    track: Track,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onSave: (Track) -> Unit,
) {
    var title by rememberSaveable(track.title) { mutableStateOf(track.title) }
    var albumPosition by rememberSaveable(track.albumPosition) { mutableStateOf(track.albumPosition?.toString() ?: "") }
    var artist by rememberSaveable(track.artist) { mutableStateOf(track.artist ?: "") }
    var discNumber by rememberSaveable(track.discNumber) { mutableStateOf(track.discNumber?.toString() ?: "") }
    var year by rememberSaveable(track.year) { mutableStateOf(track.year?.toString() ?: "") }

    AlertDialog(
        modifier = modifier.padding(20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onCancel,
        shape = MaterialTheme.shapes.small,
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        track.copy(
                            title = title,
                            albumPosition = albumPosition.toIntOrNull(),
                            artist = artist.takeIf { it.isNotBlank() },
                            discNumber = discNumber.toIntOrNull(),
                            year = year.toIntOrNull(),
                        )
                    )
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.artist)) },
                        modifier = Modifier.weight(0.7f),
                    )
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
