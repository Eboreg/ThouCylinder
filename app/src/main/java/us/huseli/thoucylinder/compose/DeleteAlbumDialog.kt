package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R


@Composable
fun DeleteAlbumDialog(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onConfirm: (removeFromLibrary: Boolean, deleteFiles: Boolean) -> Unit,
) {
    var removeFromLibrary by rememberSaveable { mutableStateOf(true) }
    var deleteFiles by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        shape = ShapeDefaults.ExtraSmall,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(removeFromLibrary, deleteFiles) },
                content = { Text(text = stringResource(R.string.do_it)) },
                enabled = removeFromLibrary || deleteFiles,
            )
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.what_do_you_want_to_do),
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                CheckboxWithText(
                    text = stringResource(R.string.remove_album_from_library),
                    checked = removeFromLibrary,
                    onCheckedChange = { removeFromLibrary = !removeFromLibrary },
                )
                CheckboxWithText(
                    text = stringResource(R.string.delete_local_album_files),
                    checked = deleteFiles,
                    onCheckedChange = { deleteFiles = !deleteFiles },
                )
            }
        }
    )
}
