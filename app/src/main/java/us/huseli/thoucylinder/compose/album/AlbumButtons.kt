package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Bookmark
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks

@Composable
fun AlbumButtons(
    isLocal: Boolean,
    isInLibrary: Boolean,
    isDownloading: Boolean,
    isPartiallyDownloaded: Boolean,
    callbacks: AlbumCallbacks,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Row {
            if (isDownloading) {
                IconButton(
                    onClick = callbacks.onCancelDownloadClick,
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Cancel,
                            contentDescription = stringResource(R.string.cancel_download),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier,
                )
            } else {
                if (!isLocal) {
                    if (isInLibrary) IconButton(
                        onClick = callbacks.onRemoveFromLibraryClick,
                        content = {
                            Icon(
                                imageVector = Icons.Sharp.Bookmark,
                                contentDescription = stringResource(R.string.remove_from_library),
                                modifier = Modifier.size(iconSize),
                            )
                        },
                        modifier = modifier,
                    )
                    else IconButton(
                        onClick = callbacks.onAddToLibraryClick,
                        content = {
                            Icon(
                                imageVector = Icons.Sharp.BookmarkBorder,
                                contentDescription = stringResource(R.string.add_to_library),
                                modifier = Modifier.size(iconSize),
                            )
                        },
                        modifier = modifier,
                    )
                }

                if (!isLocal || isPartiallyDownloaded) IconButton(
                    onClick = callbacks.onDownloadClick,
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Download,
                            contentDescription = stringResource(R.string.download),
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier,
                )
            }

            IconButton(
                onClick = callbacks.onPlayClick,
                content = {
                    Icon(
                        imageVector = Icons.Sharp.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        modifier = Modifier.size(iconSize),
                    )
                },
                modifier = modifier,
            )
        }

        Row {
            AlbumContextMenuWithButton(
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                callbacks = callbacks,
                light = true,
                isPartiallyDownloaded = isPartiallyDownloaded,
                buttonIconSize = 30.dp,
            )
        }
    }
}
