package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.umlautify

@Composable
fun EditAlbumTrackSection(
    track: Track,
    albumCombo: AlbumWithTracksCombo,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Track) -> Unit,
) {
    var title by rememberSaveable(track.title) { mutableStateOf(track.title) }
    var artist by rememberSaveable(track.artist) { mutableStateOf(track.artist) }
    var year by rememberSaveable { mutableStateOf(track.year) }
    var editMode by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!editMode) {
                Text(track.toString(showYear = true, albumCombo = albumCombo).umlautify())
            } else {
                Row {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.title)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(60.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { OutlinedTextFieldLabel(text = stringResource(R.string.title)) },
                        enabled = enabled,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(
                        value = artist ?: "",
                        onValueChange = { value -> artist = value.nullIfBlank() },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f).height(60.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) },
                        enabled = enabled,
                    )
                    OutlinedTextField(
                        value = year?.toString() ?: "",
                        onValueChange = { year = it.toIntOrNull() },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.year)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.3f).height(60.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { OutlinedTextFieldLabel(text = stringResource(R.string.year)) },
                        enabled = enabled,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (editMode) {
                SmallButton(
                    onClick = {
                        onChange(track.copy(title = title, artist = artist, year = year))
                        editMode = false
                    },
                    text = stringResource(R.string.save),
                )
                SmallOutlinedButton(
                    onClick = {
                        editMode = false
                        title = track.title
                        artist = track.artist
                        year = track.year
                    },
                    text = stringResource(R.string.cancel),
                )
            } else {
                SmallOutlinedButton(
                    onClick = { editMode = true },
                    text = stringResource(R.string.edit),
                )
            }
        }
    }
}
