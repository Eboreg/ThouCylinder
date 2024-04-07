package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AutocompleteTextField
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.stringResource

data class AlbumTrackSaveData(val title: String, val year: Int?, val artistNames: ImmutableCollection<String>)

@Composable
fun EditIndividualAlbumTrackSection(
    track: Track,
    trackComboString: String,
    artistNames: ImmutableList<String>,
    enabled: Boolean,
    getArtistNameSuggestions: (String) -> List<String>,
    onSaveClick: (AlbumTrackSaveData) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable { mutableStateOf(track.title) }
    var currentArtistNames by rememberSaveable { mutableStateOf(artistNames.toList()) }
    var year by rememberSaveable { mutableStateOf(track.year) }
    var editMode by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!editMode) {
                Text(trackComboString)
            } else {
                Row {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.title)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { OutlinedTextFieldLabel(text = stringResource(R.string.title)) },
                        enabled = enabled,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        currentArtistNames.fastForEachIndexed { index, artistName ->
                            val onTextChange: (String) -> Unit = {
                                currentArtistNames = currentArtistNames.toMutableList().apply { set(index, it) }
                            }

                            AutocompleteTextField(
                                initial = artistName,
                                getSuggestions = getArtistNameSuggestions,
                                onSelect = onTextChange,
                                onTextChange = onTextChange,
                            ) { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) }
                        }
                    }
                    OutlinedTextField(
                        value = year?.toString() ?: "",
                        onValueChange = { year = it.toIntOrNull() },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.year)) },
                        singleLine = true,
                        modifier = Modifier.weight(0.3f),
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
                        onSaveClick(
                            AlbumTrackSaveData(
                                title = title,
                                year = year,
                                artistNames = currentArtistNames.toImmutableList(),
                            )
                        )
                        editMode = false
                    },
                    text = stringResource(R.string.save),
                )
                SmallOutlinedButton(
                    onClick = {
                        editMode = false
                        title = track.title
                        currentArtistNames = artistNames
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
