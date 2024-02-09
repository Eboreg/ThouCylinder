package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel
import java.util.UUID

@Composable
fun EditAlbumTracksDialog(
    albumId: UUID,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel,
) {
    val albumWithTracks by viewModel.flowAlbumWithTracks(albumId).collectAsStateWithLifecycle(null)

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.edit_album_tracks)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose, content = { Text(stringResource(R.string.close)) }) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                albumWithTracks?.also { combo ->
                    items(combo.tracks) { track ->
                        EditAlbumTrackSection(
                            track = track,
                            albumCombo = combo,
                            enabled = true,
                            onChange = {
                                viewModel.saveTrack(it)
                                viewModel.tagAlbumTrack(combo, it)
                            },
                        )
                    }
                }
            }
        }
    )
}
