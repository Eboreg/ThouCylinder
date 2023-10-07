package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.AbstractPlaylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    playlists: List<AbstractPlaylist>,
    onSelect: (AbstractPlaylist) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPlaylist by rememberSaveable { mutableStateOf<AbstractPlaylist?>(null) }

    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        onDismissRequest = onCancel,
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            TextButton(
                onClick = { selectedPlaylist?.also(onSelect) },
                enabled = selectedPlaylist != null,
                content = { Text(stringResource(R.string.save)) },
            )
        },
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }

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
        },
    )
}
