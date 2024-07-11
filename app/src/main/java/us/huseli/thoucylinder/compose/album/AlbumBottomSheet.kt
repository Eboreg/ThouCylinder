package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.LocalThemeSizes
import us.huseli.thoucylinder.compose.utils.BottomSheetItem
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.album.IAlbumUiState
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.artist.ISavedArtistCredit
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumBottomSheet(
    uiState: IAlbumUiState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val callbacks = LocalAlbumCallbacks.current
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
            Thumbnail(model = uiState, placeholderIcon = Icons.Sharp.Album)
            Column {
                Text(
                    text = uiState.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (uiState.artistString != null) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                uiState.artistString?.also { artist ->
                    Text(
                        text = artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = FistopyTheme.bodyStyles.primarySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(10.dp))

        if (uiState.isPlayable) BottomSheetItem(
            icon = Icons.Sharp.PlayArrow,
            text = stringResource(R.string.play),
            onClick = {
                callbacks.onPlayClick(uiState.albumId)
                onDismissRequest()
            },
        )
        if (uiState.isPlayable) BottomSheetItem(
            icon = Icons.AutoMirrored.Sharp.PlaylistPlay,
            text = stringResource(R.string.enqueue),
            onClick = {
                callbacks.onEnqueueClick(uiState.albumId)
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.start_radio),
            icon = Icons.Sharp.Radio,
            onClick = {
                callbacks.onStartRadioClick(uiState.albumId)
                onDismissRequest()
            },
        )
        if (!uiState.isInLibrary) BottomSheetItem(
            text = stringResource(R.string.add_to_library),
            icon = Icons.Sharp.BookmarkBorder,
            onClick = {
                callbacks.onAddToLibraryClick(uiState.albumId)
                onDismissRequest()
            },
        )
        if (uiState.isDownloadable) BottomSheetItem(
            text = stringResource(R.string.download),
            icon = Icons.Sharp.Download,
            onClick = {
                callbacks.onDownloadClick(uiState.albumId)
                onDismissRequest()
            },
        )
        if (uiState.isInLibrary) BottomSheetItem(
            text = stringResource(R.string.edit),
            icon = Icons.Sharp.Edit,
            onClick = {
                callbacks.onEditClick(uiState.albumId)
                onDismissRequest()
            },
        )
        BottomSheetItem(
            text = stringResource(R.string.add_to_playlist),
            icon = Icons.AutoMirrored.Sharp.PlaylistAdd,
            onClick = {
                callbacks.onAddToPlaylistClick(uiState.albumId)
                onDismissRequest()
            },
        )

        uiState.artists.forEach { albumArtist ->
            val artistId = if (albumArtist is ISavedArtistCredit) albumArtist.artistId else null

            if (artistId != null) BottomSheetItem(
                text = stringResource(R.string.go_to_x, albumArtist.name),
                icon = Icons.Sharp.InterpreterMode,
                onClick = {
                    callbacks.onArtistClick(artistId)
                    onDismissRequest()
                },
            )
        }

        uiState.youtubeWebUrl?.also {
            BottomSheetItem(
                text = stringResource(R.string.play_on_youtube),
                icon = painterResource(R.drawable.youtube),
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                },
            )
        }

        uiState.spotifyWebUrl?.also {
            BottomSheetItem(
                text = stringResource(R.string.play_on_spotify),
                icon = painterResource(R.drawable.spotify),
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                },
            )
        }

        if (uiState.isInLibrary) BottomSheetItem(
            text = stringResource(R.string.delete),
            icon = Icons.Sharp.Delete,
            onClick = {
                callbacks.onDeleteClick(uiState.albumId)
                onDismissRequest()
            },
        )
    }
}

@Composable
fun AlbumBottomSheetWithButton(uiState: IAlbumUiState, modifier: Modifier = Modifier) {
    val sizes = LocalThemeSizes.current
    var isVisible by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier.size(sizes.largerIconButton).clip(CircleShape),
        onClick = { isVisible = !isVisible },
        content = {
            if (isVisible) AlbumBottomSheet(
                uiState = uiState,
                onDismissRequest = { isVisible = false },
            )
            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(sizes.largerIconButtonIcon))
        }
    )
}
