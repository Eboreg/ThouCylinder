package us.huseli.thoucylinder.compose.album

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Album

@Composable
fun DeleteAlbumDialog(
    album: Album,
    onCancel: () -> Unit,
    onDeleteAlbumClick: () -> Unit,
    onDeleteAlbumAndFilesClick: () -> Unit,
    onDeleteFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        title = { Text(text = "Delete $album") },
        confirmButton = {
            TextButton(onClick = onDeleteAlbumAndFilesClick) { Text(stringResource(R.string.remove_album_and_delete_files)) }
            TextButton(onClick = onDeleteAlbumClick) { Text(stringResource(R.string.only_remove_album)) }
            if (album.isOnYoutube)
                TextButton(onClick = onDeleteFilesClick) { Text(stringResource(R.string.only_delete_files)) }
        },
        text = { Text(text = stringResource(R.string.what_do_you_want_to_do)) },
    )
}
