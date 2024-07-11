package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.BottomSheetItem
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.dataclasses.track.LocalTrackCallbacks
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackBottomSheet(
    state: AbstractTrackUiState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hideAlbum: Boolean = false,
    extraItems: @Composable () -> Unit = {},
) {
    val callbacks = LocalTrackCallbacks.current
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Thumbnail(model = state, placeholderIcon = Icons.Sharp.MusicNote)
            Column {
                val secondRow = listOfNotNull(state.artistString, state.albumTitle)
                    .joinToString(" • ")
                    .nullIfBlank()

                Text(
                    text = state.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (secondRow != null) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (secondRow != null) Text(
                    text = secondRow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FistopyTheme.bodyStyles.primarySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(10.dp))

        if (state.isPlayable) callbacks.onPlayClick?.also { onPlayClick ->
            BottomSheetItem(
                icon = Icons.Sharp.PlayArrow,
                text = stringResource(R.string.play),
                onClick = {
                    onPlayClick(state)
                    onDismissRequest()
                },
            )
        }

        if (state.isPlayable) callbacks.onEnqueueClick?.also { onEnqueueClick ->
            BottomSheetItem(
                icon = Icons.AutoMirrored.Sharp.PlaylistPlay,
                text = stringResource(R.string.enqueue),
                onClick = {
                    onEnqueueClick(state)
                    onDismissRequest()
                },
            )
        }

        BottomSheetItem(
            icon = Icons.Sharp.Radio,
            text = stringResource(R.string.start_radio),
            onClick = {
                callbacks.onStartRadioClick(state)
                onDismissRequest()
            },
        )

        BottomSheetItem(
            icon = Icons.Sharp.Info,
            text = stringResource(R.string.track_information),
            onClick = {
                callbacks.onShowInfoClick(state)
                onDismissRequest()
            },
        )

        if (!hideAlbum) state.albumId?.also { albumId ->
            BottomSheetItem(
                icon = Icons.Sharp.Album,
                text = stringResource(R.string.go_to_album),
                onClick = {
                    callbacks.onGotoAlbumClick(albumId)
                    onDismissRequest()
                },
            )
        }

        if (state.isInLibrary) BottomSheetItem(
            icon = Icons.Sharp.Edit,
            text = stringResource(R.string.edit),
            onClick = {
                callbacks.onEditClick(state)
                onDismissRequest()
            },
        )

        if (state.isDownloadable) BottomSheetItem(
            icon = Icons.Sharp.Download,
            text = stringResource(R.string.download),
            onClick = {
                callbacks.onDownloadClick(state)
                onDismissRequest()
            },
        )

        BottomSheetItem(
            icon = Icons.AutoMirrored.Sharp.PlaylistAdd,
            text = stringResource(R.string.add_to_playlist),
            onClick = {
                callbacks.onAddToPlaylistClick(state)
                onDismissRequest()
            },
        )

        state.artists.forEach { artist ->
            if (artist.id != null) {
                BottomSheetItem(
                    icon = Icons.Sharp.InterpreterMode,
                    text = stringResource(R.string.go_to_x, artist.name),
                    onClick = {
                        callbacks.onGotoArtistClick(artist.id)
                        onDismissRequest()
                    },
                )
            }
        }

        state.youtubeWebUrl?.also {
            BottomSheetItem(
                icon = painterResource(R.drawable.youtube),
                text = stringResource(R.string.play_on_youtube),
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                },
            )
        }
        state.spotifyWebUrl?.also {
            BottomSheetItem(
                icon = painterResource(R.drawable.spotify),
                text = stringResource(R.string.play_on_spotify),
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                },
            )
        }

        extraItems()
    }
}

@Composable
fun TrackBottomSheetWithButton(
    state: AbstractTrackUiState,
    modifier: Modifier = Modifier,
    hideAlbum: Boolean = false,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 30.dp,
    extraItems: @Composable () -> Unit = {},
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier.size(buttonSize),
        onClick = { isMenuShown = !isMenuShown },
        content = {
            if (isMenuShown) TrackBottomSheet(
                state = state,
                onDismissRequest = { isMenuShown = false },
                hideAlbum = hideAlbum,
                extraItems = extraItems,
            )
            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(iconSize))
        }
    )
}
