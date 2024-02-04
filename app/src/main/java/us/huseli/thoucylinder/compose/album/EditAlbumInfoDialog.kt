package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.AddCircle
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CompactTextField
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.SmallButton
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.slice
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel
import java.util.UUID

data class GenreUI(
    val genre: Genre,
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
    val allGenres by viewModel.allGenres.collectAsStateWithLifecycle(emptyList())
    val density = LocalDensity.current
    val levenshtein = LevenshteinDistance()
    val totalAreaSize by viewModel.totalAreaSize.collectAsStateWithLifecycle(DpSize.Zero)
    var dialogSize by remember { mutableStateOf(DpSize.Zero) }

    albumWithTracks?.also { pojo ->
        var title by rememberSaveable { mutableStateOf(pojo.album.title) }
        var artist by rememberSaveable { mutableStateOf(pojo.album.artist) }
        var year by rememberSaveable { mutableStateOf(pojo.album.year) }
        var genres by rememberSaveable { mutableStateOf(pojo.genres.map { GenreUI(it) }) }
        val allGenreNames by remember(allGenres, genres) {
            mutableStateOf(
                allGenres.map { it.genreName }
                    .toSet()
                    .plus(genres.map { it.genre.genreName })
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
                        val newPojo = pojo.copy(
                            album = pojo.album.copy(artist = artist, title = title, year = year),
                            genres = genres.map { it.genre },
                        )

                        viewModel.saveAlbumPojo(newPojo)
                        viewModel.tagAlbumTracks(newPojo)
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
                        genres.forEachIndexed { index, genre ->
                            GenreChip(
                                genre = genre.genre,
                                onSave = {
                                    genres = genres.toMutableList().apply {
                                        removeAt(index)
                                        if (it.isNotEmpty()) add(index, GenreUI(Genre(it)))
                                    }
                                },
                                onDelete = {
                                    genres = genres.toMutableList().apply { removeAt(index) }
                                },
                                getSuggestions = { genreName ->
                                    allGenreNames.filter { it.contains(genreName, true) }
                                        .sortedBy { levenshtein.apply(genreName.lowercase(), it.lowercase()) }
                                        .slice(0, 10)
                                },
                                focus = genre.focus,
                                totalAreaSize = totalAreaSize,
                                dialogSize = dialogSize,
                            )
                        }
                        SmallButton(
                            onClick = {
                                genres = genres.toMutableList().apply { add(GenreUI(Genre(""), true)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreChip(
    genre: Genre,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    getSuggestions: (String) -> List<String>,
    totalAreaSize: DpSize,
    dialogSize: DpSize,
    modifier: Modifier = Modifier,
    focus: Boolean = false,
) {
    val density = LocalDensity.current
    val dialogTop = (totalAreaSize.height - dialogSize.height) / 2
    val focusRequester = remember { FocusRequester() }
    var editMode by rememberSaveable { mutableStateOf(focus) }
    var positionInRoot by remember { mutableStateOf(DpOffset.Zero) }
    var suggestions by remember { mutableStateOf(getSuggestions(genre.genreName)) }

    val absolutePosition = dialogTop + positionInRoot.y
    val maxPopupHeight = totalAreaSize.height - absolutePosition - 32.dp

    LaunchedEffect(editMode) {
        if (editMode) focusRequester.requestFocus()
    }

    InputChip(
        modifier = modifier,
        selected = editMode,
        onClick = { editMode = true },
        label = {
            CompactTextField(
                enabled = editMode,
                value = TextFieldValue(genre.genreName, TextRange(genre.genreName.length)),
                onImeAction = {
                    editMode = false
                    onSave(it.text)
                },
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        positionInRoot = with(density) { DpOffset(pos.x.toDp(), pos.y.toDp()) }
                    },
                showClearIcon = false,
                focusRequester = focusRequester,
                onFocusChanged = { if (!it.isFocused) editMode = false },
                onChange = {
                    suggestions = getSuggestions(it.text)
                }
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Sharp.Close,
                contentDescription = null,
                modifier = Modifier.clickable { onDelete() },
            )
        }
    )

    if (editMode) {
        Popup(onDismissRequest = { editMode = false }) {
            Card(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(top = 32.dp).heightIn(max = maxPopupHeight),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    suggestions.forEach {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    editMode = false
                                    onSave(it)
                                },
                        ) {
                            Text(text = it, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}
