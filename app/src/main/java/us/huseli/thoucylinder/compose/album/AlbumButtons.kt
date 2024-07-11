package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.LargerIconButton
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun AlbumButtons(uiState: AlbumUiState, isDownloading: Boolean, modifier: Modifier = Modifier, iconSpacing: Dp = 5.dp) {
    val callbacks = LocalAlbumCallbacks.current

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(iconSpacing)) {
            LargerIconButton(
                icon = Icons.Sharp.Radio,
                onClick = { callbacks.onStartRadioClick(uiState.albumId) },
                description = stringResource(R.string.start_radio),
            )

            if (isDownloading) {
                LargerIconButton(
                    icon = Icons.Sharp.Cancel,
                    onClick = { callbacks.onCancelDownloadClick(uiState.albumId) },
                    description = stringResource(R.string.cancel_download),
                    iconTint = MaterialTheme.colorScheme.error,
                )
            } else {
                if (!uiState.isInLibrary) LargerIconButton(
                    icon = Icons.Sharp.BookmarkBorder,
                    onClick = { callbacks.onAddToLibraryClick(uiState.albumId) },
                    description = stringResource(R.string.add_to_library),
                )

                if (uiState.isDownloadable) LargerIconButton(
                    icon = Icons.Sharp.Download,
                    onClick = { callbacks.onDownloadClick(uiState.albumId) },
                    description = stringResource(R.string.download_album),
                )
            }
        }

        if (uiState.isPlayable) LargerIconButton(
            icon = Icons.Sharp.PlayArrow,
            onClick = { callbacks.onPlayClick(uiState.albumId) },
            description = stringResource(R.string.play_album),
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        )
    }
}
