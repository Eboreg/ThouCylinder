package us.huseli.thoucylinder.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Track
import java.util.UUID


@Composable
fun TrackContextMenu(
    track: Track,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    isShown: Boolean,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onDismissRequest: () -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
) {
    var isInfoDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isInfoDialogOpen) {
        TrackInfoDialog(track = track, onClose = { isInfoDialogOpen = false })
    }

    DropdownMenu(
        modifier = modifier,
        expanded = isShown,
        onDismissRequest = onDismissRequest,
        offset = offset,
    ) {
        if (track.isOnYoutube && !track.isDownloaded) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download)) },
                leadingIcon = { Icon(Icons.Sharp.Download, null) },
                onClick = {
                    onDownloadClick()
                    onDismissRequest()
                },
            )
        }
        if (onGotoArtistClick != null) {
            track.artist?.also { artist ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.go_to_artist)) },
                    leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                    onClick = {
                        onGotoArtistClick(artist)
                        onDismissRequest()
                    },
                )
            }
        }
        if (onGotoAlbumClick != null) {
            track.albumId?.also { albumId ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.go_to_album)) },
                    leadingIcon = { Icon(Icons.Sharp.Album, null) },
                    onClick = {
                        onGotoAlbumClick(albumId)
                        onDismissRequest()
                    },
                )
            }
        }
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.track_information)) },
            leadingIcon = { Icon(Icons.Sharp.Info, null) },
            onClick = {
                isInfoDialogOpen = true
                onDismissRequest()
            },
        )
    }
}


@Composable
fun TrackContextMenuWithButton(
    track: Track,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier,
        onClick = { isMenuShown = !isMenuShown },
        content = {
            Icon(Icons.Sharp.MoreVert, null)
            TrackContextMenu(
                track = track,
                onDownloadClick = onDownloadClick,
                onDismissRequest = { isMenuShown = false },
                isShown = isMenuShown,
                onGotoArtistClick = onGotoArtistClick,
                onGotoAlbumClick = onGotoAlbumClick,
                offset = offset,
            )
        }
    )
}
