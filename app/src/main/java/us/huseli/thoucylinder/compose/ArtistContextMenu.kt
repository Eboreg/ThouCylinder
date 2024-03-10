package us.huseli.thoucylinder.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

@Composable
fun ArtistContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onPlayClick: () -> Unit,
    onStartRadioClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSpotifyClick: (() -> Unit)? = null,
) {
    DropdownMenu(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        expanded = expanded,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.play)) },
            leadingIcon = { Icon(Icons.Sharp.PlayArrow, null) },
            onClick = {
                onPlayClick()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.enqueue)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, null) },
            onClick = {
                onEnqueueClick()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.start_radio)) },
            leadingIcon = { Icon(Icons.Sharp.Radio, null) },
            onClick = {
                onStartRadioClick()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, null) },
            onClick = {
                onAddToPlaylistClick()
                onDismissRequest()
            }
        )
        onSpotifyClick?.also {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.browse_on_spotify)) },
                leadingIcon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = {
                    it()
                    onDismissRequest()
                }
            )
        }
    }
}
