package us.huseli.thoucylinder.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Album

@Composable
fun AlbumContextMenu(
    album: Album,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEnqueueNextClick: () -> Unit,
    onPlayClick: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
) {
    DropdownMenu(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        expanded = expanded,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.play)) },
            leadingIcon = { Icon(Icons.Sharp.PlayArrow, null) },
            onClick = {
                onPlayClick()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.play_next)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, null) },
            onClick = {
                onEnqueueNextClick()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, null) },
            onClick = {
                onAddToPlaylistClick()
                onDismissRequest()
            }
        )

        if (onArtistClick != null) {
            album.artist?.also { artist ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.go_to_artist)) },
                    leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                    onClick = {
                        onArtistClick(artist)
                        onDismissRequest()
                    },
                )
            }
        }
    }
}
