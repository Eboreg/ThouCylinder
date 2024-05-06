package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtist
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun TrackContextMenu(
    trackId: String,
    artists: ImmutableCollection<AbstractArtist>,
    isShown: Boolean,
    isDownloadable: Boolean,
    isInLibrary: Boolean,
    isPlayable: Boolean,
    callbacks: TrackCallbacks,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    hideAlbum: Boolean = false,
    youtubeWebUrl: String? = null,
    spotifyWebUrl: String? = null,
    extraItems: @Composable () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    DropdownMenu(
        modifier = modifier,
        expanded = isShown,
        onDismissRequest = onDismissRequest,
        offset = offset,
    ) {
        if (isPlayable) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.enqueue)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, null) },
                onClick = {
                    callbacks.onEnqueueClick(trackId)
                    onDismissRequest()
                }
            )
        }

        DropdownMenuItem(
            text = { Text(stringResource(R.string.start_radio)) },
            leadingIcon = { Icon(Icons.Sharp.Radio, null) },
            onClick = {
                callbacks.onStartRadioClick(trackId)
                onDismissRequest()
            },
        )

        if (isDownloadable) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.download)) },
                leadingIcon = { Icon(Icons.Sharp.Download, null) },
                onClick = {
                    callbacks.onDownloadClick(trackId)
                    onDismissRequest()
                },
            )
        }

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, null) },
            onClick = {
                callbacks.onAddToPlaylistClick(trackId)
                onDismissRequest()
            }
        )

        artists.forEach { trackArtist ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.go_to_x, trackArtist.name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                onClick = {
                    callbacks.onGotoArtistClick(trackArtist.artistId)
                    onDismissRequest()
                },
            )
        }

        youtubeWebUrl?.also {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play_on_youtube)) },
                leadingIcon = { Icon(painterResource(R.drawable.youtube), null) },
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                }
            )
        }

        spotifyWebUrl?.also {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play_on_spotify)) },
                leadingIcon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                }
            )
        }

        if (!hideAlbum) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.go_to_album)) },
                leadingIcon = { Icon(Icons.Sharp.Album, null) },
                onClick = {
                    callbacks.onGotoAlbumClick(trackId)
                    onDismissRequest()
                },
            )
        }

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.track_information)) },
            leadingIcon = { Icon(Icons.Sharp.Info, null) },
            onClick = {
                callbacks.onShowInfoClick(trackId)
                onDismissRequest()
            },
        )

        if (isInLibrary) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.edit_track)) },
                leadingIcon = { Icon(Icons.Sharp.Edit, null) },
                onClick = {
                    callbacks.onEditClick(trackId)
                    onDismissRequest()
                },
            )
        }

        extraItems()
    }
}

@Composable
fun TrackContextButtonWithMenu(
    trackId: String,
    artists: ImmutableCollection<AbstractArtist>,
    isDownloadable: Boolean,
    isInLibrary: Boolean,
    isPlayable: Boolean,
    callbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    hideAlbum: Boolean = false,
    youtubeWebUrl: String? = null,
    spotifyWebUrl: String? = null,
    extraItems: @Composable () -> Unit = {},
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier.size(32.dp, 40.dp),
        onClick = { isMenuShown = !isMenuShown },
        content = {
            Icon(Icons.Sharp.MoreVert, null)
            TrackContextMenu(
                artists = artists,
                callbacks = callbacks,
                onDismissRequest = { isMenuShown = false },
                isShown = isMenuShown,
                offset = offset,
                isDownloadable = isDownloadable,
                hideAlbum = hideAlbum,
                extraItems = extraItems,
                isInLibrary = isInLibrary,
                youtubeWebUrl = youtubeWebUrl,
                spotifyWebUrl = spotifyWebUrl,
                trackId = trackId,
                isPlayable = isPlayable,
            )
        }
    )
}
