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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.slice
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AutocompleteChip
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel
import java.util.UUID

data class TagUI(
    val tag: Tag,
    val focus: Boolean = false,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAlbumInfoDialog(
    albumId: UUID,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    val albumWithTracks by viewModel.flowAlbumWithTracks(albumId).collectAsStateWithLifecycle(null)
    val allTags by viewModel.allTags.collectAsStateWithLifecycle(emptyList())
    val density = LocalDensity.current
    val levenshtein = LevenshteinDistance()
    val totalAreaSize by viewModel.totalAreaSize.collectAsStateWithLifecycle(DpSize.Zero)
    var dialogSize by remember { mutableStateOf(DpSize.Zero) }

    albumWithTracks?.also { combo ->
        var title by rememberSaveable { mutableStateOf(combo.album.title) }
        var artist by rememberSaveable { mutableStateOf(combo.album.artist) }
        var year by rememberSaveable { mutableStateOf(combo.album.year) }
        var tags by rememberSaveable { mutableStateOf(combo.tags.map { TagUI(it) }) }
        val allTagNames by remember(allTags, tags) {
            mutableStateOf(
                allTags.map { it.name }
                    .toSet()
                    .plus(tags.map { it.tag.name })
            )
        }

        AlertDialog(
            modifier = modifier
                .padding(10.dp)
                .onGloballyPositioned { coords ->
                    val bounds = coords.boundsInWindow()
                    dialogSize = with(density) { DpSize(bounds.width.toDp(), bounds.height.toDp()) }
                },
            title = { Text(stringResource(R.string.edit_album)) },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            shape = MaterialTheme.shapes.small,
            onDismissRequest = onClose,
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCombo = combo.copy(
                            album = combo.album.copy(artist = artist, title = title, year = year),
                            tags = tags.map { it.tag },
                        )

                        viewModel.updateAlbumCombo(newCombo)
                        viewModel.tagAlbumTracks(newCombo)
                        onClose()
                    },
                    content = { Text(text = stringResource(R.string.save)) },
                )
            },
            dismissButton = { TextButton(onClick = onClose, content = { Text(stringResource(R.string.cancel)) }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = artist ?: "",
                            onValueChange = { artist = it },
                            label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_artist)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.7f),
                        )
                        OutlinedTextField(
                            value = year?.toString() ?: "",
                            onValueChange = { year = it.toIntOrNull() },
                            label = { OutlinedTextFieldLabel(text = stringResource(R.string.year)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        )
                    }
                    Text(stringResource(R.string.genres), style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        tags.forEachIndexed { index, tagUI ->
                            AutocompleteChip(
                                text = tagUI.tag.name,
                                onSave = {
                                    tags = tags.toMutableList().apply {
                                        val tagUi = removeAt(index)
                                        if (it.isNotEmpty()) add(index, tagUi.copy(tag = tagUi.tag.copy(name = it)))
                                    }
                                },
                                onDelete = {
                                    tags = tags.toMutableList().apply { removeAt(index) }
                                },
                                getSuggestions = { tagName ->
                                    allTagNames.filter { it.contains(tagName, true) }
                                        .sortedBy { levenshtein.apply(tagName.lowercase(), it.lowercase()) }
                                        .slice(0, 10)
                                },
                                focus = tagUI.focus,
                                totalAreaSize = totalAreaSize,
                                dialogSize = dialogSize,
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
}
