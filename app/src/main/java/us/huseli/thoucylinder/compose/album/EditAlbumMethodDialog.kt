package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel

enum class EditAlbumDialogType { ALBUM, TRACKS, COVER }

@Composable
fun EditAlbumMethodButton(
    onClick: () -> Unit,
    icon: ImageVector,
    textRes: Int,
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(10.dp),
    ) {
        Icon(icon, null, modifier = Modifier.padding(end = 15.dp))
        Text(stringResource(textRes), modifier = Modifier.weight(1f))
    }
}

@Composable
fun EditAlbumMethodDialog(
    albumId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    var openDialogType by rememberSaveable { mutableStateOf<EditAlbumDialogType?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(albumId) {
        viewModel.setAlbumId(albumId)
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.small,
        dismissButton = { CancelButton(onClick = onClose, text = stringResource(R.string.close)) },
        confirmButton = {},
        title = {
            uiState?.title?.also {
                Text(text = it.umlautify(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                EditAlbumMethodButton(
                    onClick = { openDialogType = EditAlbumDialogType.ALBUM },
                    icon = Icons.Sharp.Album,
                    textRes = R.string.edit_album,
                )
                EditAlbumMethodButton(
                    onClick = { openDialogType = EditAlbumDialogType.TRACKS },
                    icon = Icons.AutoMirrored.Sharp.QueueMusic,
                    textRes = R.string.edit_tracks,
                )
                EditAlbumMethodButton(
                    onClick = { openDialogType = EditAlbumDialogType.COVER },
                    icon = Icons.Sharp.Photo,
                    textRes = R.string.select_album_cover,
                )
            }
        },
    )

    when (openDialogType) {
        EditAlbumDialogType.ALBUM -> {
            uiState?.also {
                EditAlbumInfoDialog(
                    uiState = it,
                    onClose = { openDialogType = null },
                    viewModel = viewModel,
                )
            }
        }
        EditAlbumDialogType.TRACKS -> {
            uiState?.also {
                EditAlbumTracksDialog(
                    albumUiState = it,
                    onClose = { openDialogType = null },
                    viewModel = viewModel,
                )
            }
        }
        EditAlbumDialogType.COVER -> {
            EditAlbumCoverDialog(
                albumId = albumId,
                onClose = { openDialogType = null },
                viewModel = viewModel,
            )
        }
        else -> {}
    }
}
