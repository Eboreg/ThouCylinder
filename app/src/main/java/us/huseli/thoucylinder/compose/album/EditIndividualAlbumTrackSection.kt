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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AutocompleteTextField
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

data class AlbumTrackSaveData(val title: String, val year: Int?, val artistNames: Collection<String>)

@Composable
fun EditIndividualAlbumTrackSection(
    combo: TrackCombo,
    albumCombo: AlbumWithTracksCombo?,
    enabled: Boolean,
    totalAreaHeight: Dp,
    dialogHeight: Dp,
    getArtistNameSuggestions: (String) -> Collection<String>,
    onSaveClick: (AlbumTrackSaveData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialArtistNames = combo.artists.map { it.name }.takeIf { it.isNotEmpty() } ?: listOf("")
    var title by rememberSaveable { mutableStateOf(combo.track.title) }
    var artistNames by rememberSaveable { mutableStateOf(initialArtistNames) }
    var year by rememberSaveable { mutableStateOf(combo.track.year) }
    var editMode by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!editMode) {
                Text(combo.toString(showYear = true, albumCombo = albumCombo).umlautify())
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
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        artistNames.fastForEachIndexed { index, artistName ->
                            val onTextChange: (String) -> Unit = {
                                artistNames = artistNames.toMutableList().apply { set(index, it) }
                            }

                            AutocompleteTextField(
                                initial = artistName,
                                getSuggestions = getArtistNameSuggestions,
                                onSelect = onTextChange,
                                onTextChange = onTextChange,
                                totalAreaHeight = totalAreaHeight,
                                modifier = Modifier.height(60.dp),
                                rootOffsetY = (totalAreaHeight - dialogHeight) / 2,
                            ) { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) }
                        }
                    }
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
                        onSaveClick(AlbumTrackSaveData(title = title, year = year, artistNames = artistNames))
                        editMode = false
                    },
                    text = stringResource(R.string.save),
                )
                SmallOutlinedButton(
                    onClick = {
                        editMode = false
                        title = combo.track.title
                        artistNames = initialArtistNames
                        year = combo.track.year
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
