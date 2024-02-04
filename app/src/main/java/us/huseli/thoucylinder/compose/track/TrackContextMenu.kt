package us.huseli.thoucylinder.compose.track

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistPlay
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks

@Composable
fun TrackContextMenu(
    isShown: Boolean,
    isDownloadable: Boolean,
    isInLibrary: Boolean,
    callbacks: TrackCallbacks<*>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    hideAlbum: Boolean = false,
    extraItems: (@Composable () -> Unit)? = null,
) {
    DropdownMenu(
        modifier = modifier,
        expanded = isShown,
        onDismissRequest = onDismissRequest,
        offset = offset,
    ) {
        if (isDownloadable) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download)) },
                leadingIcon = { Icon(Icons.Sharp.Download, null) },
                onClick = {
                    callbacks.onDownloadClick()
                    onDismissRequest()
                },
            )
        }

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.Sharp.PlaylistAdd, null) },
            onClick = {
                callbacks.onAddToPlaylistClick()
                onDismissRequest()
            }
        )

        callbacks.onEnqueueClick?.also { onEnqueueClick ->
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.enqueue)) },
                leadingIcon = { Icon(Icons.Sharp.PlaylistPlay, null) },
                onClick = {
                    onEnqueueClick()
                    onDismissRequest()
                }
            )
        }

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

        if (!hideAlbum) {
            callbacks.onAlbumClick?.also { onAlbumClick ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.go_to_album)) },
                    leadingIcon = { Icon(Icons.Sharp.Album, null) },
                    onClick = {
                        onAlbumClick()
                        onDismissRequest()
                    },
                )
            }
        }

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.track_information)) },
            leadingIcon = { Icon(Icons.Sharp.Info, null) },
            onClick = {
                callbacks.onShowInfoClick()
                onDismissRequest()
            },
        )

        if (isInLibrary) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.edit_track)) },
                leadingIcon = { Icon(Icons.Sharp.Edit, null) },
                onClick = {
                    callbacks.onEditTrackClick()
                    onDismissRequest()
                },
            )
        }

        extraItems?.invoke()
    }
}


@Composable
fun TrackContextButtonWithMenu(
    isDownloadable: Boolean,
    isInLibrary: Boolean,
    callbacks: TrackCallbacks<*>,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    hideAlbum: Boolean = false,
    extraItems: (@Composable () -> Unit)? = null,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier,
        onClick = { isMenuShown = !isMenuShown },
        content = {
            Icon(Icons.Sharp.MoreVert, null)
            TrackContextMenu(
                callbacks = callbacks,
                onDismissRequest = { isMenuShown = false },
                isShown = isMenuShown,
                offset = offset,
                isDownloadable = isDownloadable,
                hideAlbum = hideAlbum,
                extraItems = extraItems,
                isInLibrary = isInLibrary,
            )
        }
    )
}
