package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Bookmark
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun AlbumButtons(
    isLocal: Boolean,
    isInLibrary: Boolean,
    isDownloading: Boolean,
    callbacks: AlbumCallbacks,
    modifier: Modifier = Modifier,
) {
    var isContextMenuOpen by rememberSaveable { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row {
            if (!isLocal) {
                if (isDownloading) {
                    IconButton(
                        onClick = callbacks.onCancelDownloadClick,
                        content = { Icon(Icons.Sharp.Cancel, stringResource(R.string.cancel_download)) },
                        modifier = modifier,
                    )
                } else {
                    if (isInLibrary) {
                        IconButton(
                            onClick = callbacks.onRemoveFromLibraryClick,
                            content = { Icon(Icons.Sharp.Bookmark, stringResource(R.string.remove_from_library)) },
                            modifier = modifier,
                        )
                    } else {
                        IconButton(
                            onClick = callbacks.onAddToLibraryClick,
                            content = { Icon(Icons.Sharp.BookmarkBorder, stringResource(R.string.add_to_library)) },
                            modifier = modifier,
                        )
                    }
                    IconButton(
                        onClick = callbacks.onDownloadClick,
                        content = { Icon(Icons.Sharp.Download, stringResource(R.string.download)) },
                        modifier = modifier,
                    )
                }
            }
            if (isInLibrary) {
                IconButton(
                    onClick = callbacks.onEditClick,
                    content = { Icon(Icons.Sharp.Edit, stringResource(R.string.edit)) },
                    modifier = modifier,
                )
            }
            IconButton(
                onClick = callbacks.onPlayClick,
                content = { Icon(Icons.Sharp.PlayArrow, null) },
                modifier = modifier,
            )
        }

        Row {
            IconButton(
                onClick = { isContextMenuOpen = !isContextMenuOpen },
                content = { Icon(Icons.Sharp.MoreVert, null) },
            )
            AlbumContextMenu(
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                expanded = isContextMenuOpen,
                onDismissRequest = { isContextMenuOpen = false },
                callbacks = callbacks,
                light = true,
            )
        }
    }
}
