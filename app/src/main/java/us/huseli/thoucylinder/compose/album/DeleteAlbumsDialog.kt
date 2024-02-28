package us.huseli.thoucylinder.compose.album

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource

@Composable
fun DeleteAlbumsDialog(
    count: Int,
    onCancel: () -> Unit,
    onDeleteAlbumsClick: () -> Unit,
    onDeleteAlbumsAndFilesClick: () -> Unit,
    onDeleteFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        title = { Text(text = pluralStringResource(R.plurals.delete_x_albums, count, count)) },
        confirmButton = {
            TextButton(onClick = onDeleteAlbumsAndFilesClick) {
                Text(pluralStringResource(R.plurals.remove_album_and_delete_files, count, count))
            }
            TextButton(onClick = onDeleteAlbumsClick) {
                Text(pluralStringResource(R.plurals.only_remove_album, count, count))
            }
            TextButton(onClick = onDeleteFilesClick) { Text(stringResource(R.string.only_delete_files)) }
        },
        text = { Text(text = stringResource(R.string.what_do_you_want_to_do)) },
    )
}
