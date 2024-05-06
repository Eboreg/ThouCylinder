package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtist
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun AlbumButtons(
    albumId: String,
    albumArtists: ImmutableCollection<AbstractArtist>,
    isLocal: Boolean,
    isInLibrary: Boolean,
    isDownloading: Boolean,
    isPartiallyDownloaded: Boolean,
    callbacks: AlbumCallbacks,
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    spotifyWebUrl: String? = null,
    youtubeWebUrl: String? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row {
            IconButton(
                onClick = { callbacks.onEditClick(albumId) },
                content = {
                    Icon(
                        imageVector = Icons.Sharp.Edit,
                        contentDescription = stringResource(R.string.edit_album),
                        modifier = Modifier.size(iconSize),
                    )
                }
            )
            if (isDownloading) {
                IconButton(
                    onClick = { callbacks.onCancelDownloadClick(albumId) },
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Cancel,
                            contentDescription = stringResource(R.string.cancel_download),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier.padding(horizontal = 5.dp),
                )
            } else {
                if (isInLibrary) IconButton(
                    onClick = { callbacks.onDeleteClick(albumId) },
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Delete,
                            contentDescription = stringResource(R.string.delete_album),
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier.padding(horizontal = 5.dp),
                )
                else IconButton(
                    onClick = { callbacks.onAddToLibraryClick(albumId) },
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.BookmarkBorder,
                            contentDescription = stringResource(R.string.add_to_library),
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier.padding(horizontal = 5.dp),
                )

                if (!isLocal || isPartiallyDownloaded) IconButton(
                    onClick = { callbacks.onDownloadClick(albumId) },
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Download,
                            contentDescription = stringResource(R.string.download),
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier.padding(horizontal = 5.dp),
                )
            }

            callbacks.onPlayClick?.also { onPlayClick ->
                IconButton(
                    onClick = { onPlayClick(albumId) },
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            modifier = Modifier.size(iconSize),
                        )
                    },
                    modifier = modifier.padding(horizontal = 5.dp),
                )
            }
        }

        Row {
            AlbumContextMenuWithButton(
                albumId = albumId,
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                callbacks = callbacks,
                isPartiallyDownloaded = isPartiallyDownloaded,
                albumArtists = albumArtists,
                youtubeWebUrl = youtubeWebUrl,
                spotifyWebUrl = spotifyWebUrl,
            )
        }
    }
}
