package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel
import java.util.UUID

enum class EditAlbumDialogType { ALBUM, TRACKS, COVER }

@Composable
fun EditAlbumMethodDialog(
    albumId: UUID,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    var openDialogType by rememberSaveable { mutableStateOf<EditAlbumDialogType?>(null) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.small,
        dismissButton = {
            TextButton(onClick = onClose, content = { Text(stringResource(R.string.close)) })
        },
        confirmButton = {},
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { openDialogType = EditAlbumDialogType.ALBUM },
                    content = { Text(stringResource(R.string.edit_album)) },
                    shape = MaterialTheme.shapes.extraSmall,
                )
                Button(
                    onClick = { openDialogType = EditAlbumDialogType.TRACKS },
                    content = { Text(stringResource(R.string.edit_tracks)) },
                    shape = MaterialTheme.shapes.extraSmall,
                )
                Button(
                    onClick = { openDialogType = EditAlbumDialogType.COVER },
                    content = { Text(stringResource(R.string.edit_album_cover)) },
                    shape = MaterialTheme.shapes.extraSmall,
                )
            }
        },
    )

    when (openDialogType) {
        EditAlbumDialogType.ALBUM -> {
            EditAlbumInfoDialog(
                albumId = albumId,
                onClose = { openDialogType = null },
                viewModel = viewModel,
            )
        }
        EditAlbumDialogType.TRACKS -> {
            EditAlbumTracksDialog(
                albumId = albumId,
                onClose = { openDialogType = null },
                viewModel = viewModel,
            )
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
