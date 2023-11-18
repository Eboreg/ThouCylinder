package us.huseli.thoucylinder.compose.album

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Bookmark
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks

@Composable
fun AlbumContextMenu(
    isLocal: Boolean,
    isInLibrary: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    callbacks: AlbumCallbacks,
    light: Boolean = false,
) {
    DropdownMenu(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        expanded = expanded,
    ) {
        if (!isLocal && !light) {
            if (!isInLibrary) DropdownMenuItem(
                text = { Text(text = stringResource(R.string.add_to_library)) },
                leadingIcon = { Icon(Icons.Sharp.BookmarkBorder, null) },
                onClick = {
                    callbacks.onAddToLibraryClick()
                    onDismissRequest()
                },
            )

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.download)) },
                leadingIcon = { Icon(Icons.Sharp.Download, null) },
                onClick = {
                    callbacks.onDownloadClick()
                    onDismissRequest()
                },
            )
        }

        if (!light) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.play)) },
            leadingIcon = { Icon(Icons.Sharp.PlayArrow, null) },
            onClick = {
                callbacks.onPlayClick()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.enqueue)) },
            leadingIcon = { Icon(Icons.Sharp.PlaylistPlay, null) },
            onClick = {
                callbacks.onEnqueueClick()
                onDismissRequest()
            }
        )

        if (isInLibrary) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.edit)) },
            leadingIcon = { Icon(Icons.Sharp.Edit, null) },
            onClick = {
                callbacks.onEditClick()
                onDismissRequest()
            },
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.Sharp.PlaylistAdd, null) },
            onClick = {
                callbacks.onAddToPlaylistClick()
                onDismissRequest()
            }
        )

        callbacks.onArtistClick?.also { onArtistClick ->
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.go_to_artist)) },
                leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                onClick = {
                    onArtistClick()
                    onDismissRequest()
                },
            )
        }

        if (!light && !isLocal && isInLibrary) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.remove_from_library)) },
            leadingIcon = { Icon(Icons.Sharp.Bookmark, null) },
            onClick = {
                callbacks.onRemoveFromLibraryClick()
                onDismissRequest()
            },
        )

        if (isLocal) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.delete_album)) },
            leadingIcon = { Icon(Icons.Sharp.Delete, null) },
            onClick = {
                callbacks.onDeleteClick()
                onDismissRequest()
            }
        )
    }
}

@Composable
fun AlbumContextMenuWithButton(
    isLocal: Boolean,
    isInLibrary: Boolean,
    modifier: Modifier = Modifier,
    callbacks: AlbumCallbacks,
    light: Boolean = false,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier,
        onClick = { isMenuShown = !isMenuShown },
        content = {
            Icon(Icons.Sharp.MoreVert, null)
            AlbumContextMenu(
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                expanded = isMenuShown,
                onDismissRequest = { isMenuShown = false },
                callbacks = callbacks,
                light = light,
            )
        }
    )
}
