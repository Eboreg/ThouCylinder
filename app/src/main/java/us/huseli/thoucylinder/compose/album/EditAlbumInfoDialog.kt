package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AutocompleteChip
import us.huseli.thoucylinder.compose.utils.AutocompleteTextField
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel

data class TagUI(
    val tag: Tag,
    val focus: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAlbumInfoDialog(
    albumId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    val levenshtein = LevenshteinDistance()

    val albumWithTracks by viewModel.flowAlbumWithTracks(albumId).collectAsStateWithLifecycle(null)
    val allTags by viewModel.allTags.collectAsStateWithLifecycle(emptyList())

    var updateTrackArtists by rememberSaveable { mutableStateOf(true) }
    var title by rememberSaveable(albumWithTracks) { mutableStateOf(albumWithTracks?.album?.title ?: "") }
    var artistNames by rememberSaveable(albumWithTracks) {
        mutableStateOf(albumWithTracks?.artists?.map { it.name } ?: emptyList())
    }
    var year by rememberSaveable(albumWithTracks) { mutableStateOf(albumWithTracks?.album?.year) }
    var tags by rememberSaveable(albumWithTracks) {
        mutableStateOf(albumWithTracks?.tags?.map { TagUI(it) } ?: emptyList())
    }
    val allTagNames by remember(allTags, tags) {
        mutableStateOf(
            allTags.map { it.name }
                .toSet()
                .plus(tags.map { it.tag.name })
        )
    }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.edit_album)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        confirmButton = {
            SaveButton {
                albumWithTracks?.also { combo ->
                    viewModel.updateAlbumCombo(
                        combo = combo,
                        title = title,
                        year = year,
                        artistNames = artistNames,
                        tags = tags.map { it.tag },
                        updateMatchingTrackArtists = updateTrackArtists,
                    )
                }
                onClose()
            }
        },
        dismissButton = { CancelButton(onClick = onClose) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.fillMaxWidth(0.7f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        artistNames.fastForEachIndexed { index, artistName ->
                            val onTextChange: (String) -> Unit = {
                                artistNames = artistNames.toMutableList().apply { set(index, it) }
                            }

                            AutocompleteTextField(
                                initial = artistName,
                                getSuggestions = { name -> viewModel.getArtistNameSuggestions(name) },
                                onSelect = onTextChange,
                                onTextChange = onTextChange,
                            ) { OutlinedTextFieldLabel(text = stringResource(R.string.album_artist)) }
                        }
                    }
                    OutlinedTextField(
                        value = year?.toString() ?: "",
                        onValueChange = { year = it.toIntOrNull() },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.year)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.also_update_matching_track_artists),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = updateTrackArtists, onCheckedChange = { updateTrackArtists = it })
                }

                Text(stringResource(R.string.genres), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    tags.forEachIndexed { index, tagUI ->
                        AutocompleteChip(
                            originalText = tagUI.tag.name,
                            onSave = {
                                tags = tags.toMutableList().apply {
                                    val tagUi = removeAt(index)
                                    if (it.isNotEmpty()) add(index, tagUi.copy(tag = tagUi.tag.copy(name = it.trim())))
                                }
                            },
                            onDelete = {
                                tags = tags.toMutableList().apply { removeAt(index) }
                            },
                            getSuggestions = { tagName ->
                                allTagNames.filter { it.contains(tagName, true) }
                                    .sortedBy { levenshtein.apply(tagName.lowercase(), it.lowercase()) }
                                    .take(5)
                            },
                        )
                    }
                    SmallButton(
                        onClick = {
                            tags = tags.toMutableList().apply { add(TagUI(Tag(name = ""), true)) }
                        },
                        text = stringResource(R.string.add_new),
                        leadingIcon = Icons.Sharp.AddCircle,
                    )
                }
            }
        },
    )
}
