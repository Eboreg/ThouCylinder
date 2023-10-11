package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.retaintheme.snackbar.SnackbarEngine
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    playlists: List<AbstractPlaylist>,
    onSelect: (AbstractPlaylist) -> Unit,
    onCreateNew: (Playlist) -> Unit,
    onCancel: () -> Unit,
    onGotoPlaylist: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPlaylist by rememberSaveable { mutableStateOf<AbstractPlaylist?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        onDismissRequest = onCancel,
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedPlaylist?.also { playlist ->
                        onSelect(playlist)
                        SnackbarEngine.addInfo(
                            message = context.getString(R.string.selection_was_added_to_playlist),
                            actionLabel = context.getString(R.string.go_to_playlist),
                            onActionPerformed = { onGotoPlaylist(playlist.playlistId) },
                        )
                    } ?: run {
                        if (name.isNotBlank()) {
                            val playlist = Playlist(name = name)
                            onCreateNew(playlist)
                            SnackbarEngine.addInfo(
                                message = context.getString(R.string.playlist_was_created),
                                actionLabel = context.getString(R.string.go_to_playlist),
                                onActionPerformed = { onGotoPlaylist(playlist.playlistId) },
                            )
                        }
                    }
                },
                enabled = selectedPlaylist != null || name.isNotBlank(),
                content = { Text(stringResource(R.string.save)) },
            )
        },
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    modifier = modifier.fillMaxWidth(),
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = selectedPlaylist?.toString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_a_playlist)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.exposedDropdownSize(),
                    ) {
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(text = playlist.toString()) },
                                onClick = {
                                    selectedPlaylist = playlist
                                    isDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Text(text = stringResource(R.string.or_create_a_new_one), style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                )
            }
        },
    )
}
