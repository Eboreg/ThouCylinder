package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.sharp.Radio
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit

@Composable
fun AlbumContextMenu(
    albumArtists: Collection<AlbumArtistCredit>,
    isLocal: Boolean,
    isInLibrary: Boolean,
    isPartiallyDownloaded: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    callbacks: AlbumCallbacks,
) {
    DropdownMenu(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        expanded = expanded,
    ) {
        callbacks.onPlayClick?.also { onPlayClick ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.play)) },
                leadingIcon = { Icon(Icons.Sharp.PlayArrow, null) },
                onClick = {
                    onPlayClick()
                    onDismissRequest()
                }
            )
        }

        callbacks.onEnqueueClick?.also { onEnqueueClick ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.enqueue)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, null) },
                onClick = {
                    onEnqueueClick()
                    onDismissRequest()
                }
            )
        }

        DropdownMenuItem(
            text = { Text(stringResource(R.string.start_radio)) },
            leadingIcon = { Icon(Icons.Sharp.Radio, null) },
            onClick = {
                callbacks.onStartAlbumRadioClick()
                onDismissRequest()
            }
        )

        if (!isInLibrary) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_library)) },
            leadingIcon = { Icon(Icons.Sharp.BookmarkBorder, null) },
            onClick = {
                callbacks.onAddToLibraryClick()
                onDismissRequest()
            },
        )

        if (!isLocal || isPartiallyDownloaded) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.download)) },
            leadingIcon = { Icon(Icons.Sharp.Download, null) },
            onClick = {
                callbacks.onDownloadClick()
                onDismissRequest()
            },
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
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, null) },
            onClick = {
                callbacks.onAddToPlaylistClick()
                onDismissRequest()
            }
        )

        albumArtists.forEach { albumArtist ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.go_to_x, albumArtist.name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                onClick = {
                    callbacks.onArtistClick(albumArtist.artistId)
                    onDismissRequest()
                },
            )
        }

        callbacks.onPlayOnYoutubeClick?.also { onPlayOnYoutubeClick ->
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play_on_youtube)) },
                leadingIcon = { Icon(painterResource(R.drawable.youtube), null) },
                onClick = {
                    onPlayOnYoutubeClick()
                    onDismissRequest()
                }
            )
        }

        callbacks.onPlayOnSpotifyClick?.also { onPlayOnSpotifyClick ->
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play_on_spotify)) },
                leadingIcon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = {
                    onPlayOnSpotifyClick()
                    onDismissRequest()
                }
            )
        }

        if (isInLibrary) DropdownMenuItem(
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
    albumArtists: Collection<AlbumArtistCredit>,
    isLocal: Boolean,
    isInLibrary: Boolean,
    isPartiallyDownloaded: Boolean,
    modifier: Modifier = Modifier,
    callbacks: AlbumCallbacks,
    buttonIconSize: Dp = 30.dp,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier.size(32.dp, 40.dp),
        onClick = { isMenuShown = !isMenuShown },
        content = {
            AlbumContextMenu(
                albumArtists = albumArtists,
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                expanded = isMenuShown,
                onDismissRequest = { isMenuShown = false },
                callbacks = callbacks,
                isPartiallyDownloaded = isPartiallyDownloaded,
            )
            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(buttonIconSize))
        }
    )
}
