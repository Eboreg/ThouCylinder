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
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Track
import java.util.UUID

@Composable
fun TrackContextMenu(
    track: Track,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }
    var isInfoDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isInfoDialogOpen) {
        TrackInfoDialog(track = track, onClose = { isInfoDialogOpen = false })
    }

    IconButton(
        modifier = modifier,
        onClick = { isMenuShown = !isMenuShown },
        content = {
            Icon(Icons.Sharp.MoreVert, null)
            DropdownMenu(
                expanded = isMenuShown,
                onDismissRequest = { isMenuShown = false },
            ) {
                if (track.isOnYoutube && !track.isDownloaded) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.download)) },
                        leadingIcon = { Icon(Icons.Sharp.Download, null) },
                        onClick = {
                            onDownloadClick()
                            isMenuShown = false
                        },
                    )
                }
                if (onGotoArtistClick != null && track.artist != null) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.go_to_artist)) },
                        leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                        onClick = {
                            onGotoArtistClick(track.artist)
                            isMenuShown = false
                        },
                    )
                }
                if (onGotoAlbumClick != null && track.albumId != null) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.go_to_album)) },
                        leadingIcon = { Icon(Icons.Sharp.Album, null) },
                        onClick = {
                            onGotoAlbumClick(track.albumId)
                            isMenuShown = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.track_information)) },
                    leadingIcon = { Icon(Icons.Sharp.Info, null) },
                    onClick = {
                        isInfoDialogOpen = true
                        isMenuShown = false
                    },
                )
            }
        }
    )
}
