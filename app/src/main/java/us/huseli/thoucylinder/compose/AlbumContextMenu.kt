package us.huseli.thoucylinder.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
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
            /**
             * For some completely inexplicable reason, the app crashes with "index out of bounds" if album is
             * removed from library while in album list. Apparently the lazy list logic tries to look ahead based on
             * a list where the album still exists, even though it has been removed. D-:
             * Maybe related (though I don't run the same compose versions):
             * https://issuetracker.google.com/issues/307592542
             * https://issuetracker.google.com/issues/306301019
             * https://stackoverflow.com/questions/77357872/why-is-android-compose-lazycolumn-now-failing-with-indexoutofbounds-exception-fo
             */
            /*
            if (isInLibrary) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.remove_from_library)) },
                    leadingIcon = { Icon(Icons.Sharp.Bookmark, null) },
                    onClick = {
                        callbacks.onRemoveFromLibraryClick()
                        onDismissRequest()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.add_to_library)) },
                    leadingIcon = { Icon(Icons.Sharp.BookmarkBorder, null) },
                    onClick = {
                        callbacks.onAddToLibraryClick()
                        onDismissRequest()
                    },
                )
            }
             */
            if (!isInLibrary) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.add_to_library)) },
                    leadingIcon = { Icon(Icons.Sharp.BookmarkBorder, null) },
                    onClick = {
                        callbacks.onAddToLibraryClick()
                        onDismissRequest()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.download)) },
                leadingIcon = { Icon(Icons.Sharp.Download, null) },
                onClick = {
                    callbacks.onDownloadClick()
                    onDismissRequest()
                },
            )
        }

        if (!light) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play)) },
                leadingIcon = { Icon(Icons.Sharp.PlayArrow, null) },
                onClick = {
                    callbacks.onPlayClick()
                    onDismissRequest()
                }
            )
        }

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.enqueue)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, null) },
            onClick = {
                callbacks.onEnqueueClick()
                onDismissRequest()
            }
        )

        if (isInLibrary) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.edit)) },
                leadingIcon = { Icon(Icons.Sharp.Edit, null) },
                onClick = {
                    callbacks.onEditClick()
                    onDismissRequest()
                },
            )
        }

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, null) },
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

        DropdownMenuItem(
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
