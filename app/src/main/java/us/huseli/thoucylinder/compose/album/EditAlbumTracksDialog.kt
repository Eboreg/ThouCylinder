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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel
import java.util.UUID

@Composable
fun EditAlbumTracksDialog(
    albumId: UUID,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel,
) {
    val density = LocalDensity.current

    val albumWithTracks by viewModel.flowAlbumWithTracks(albumId).collectAsStateWithLifecycle(null)
    val totalAreaSize by viewModel.totalAreaSize.collectAsStateWithLifecycle(DpSize.Zero)

    var dialogHeight by remember { mutableStateOf(0.dp) }

    AlertDialog(
        modifier = modifier
            .padding(10.dp)
            .onGloballyPositioned { coords ->
                dialogHeight = with(density) { coords.boundsInWindow().height.toDp() }
            },
        title = { Text(stringResource(R.string.edit_album_tracks)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose, content = { Text(stringResource(R.string.close)) }) },
        text = {
            albumWithTracks?.also { albumCombo ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(albumCombo.trackCombos) { combo ->
                        EditIndividualAlbumTrackSection(
                            combo = combo,
                            albumCombo = albumCombo,
                            enabled = true,
                            totalAreaHeight = totalAreaSize.height,
                            dialogHeight = dialogHeight,
                            getArtistNameSuggestions = { name -> viewModel.getArtistNameSuggestions(name) },
                            onSaveClick = { data ->
                                viewModel.updateTrackCombo(
                                    combo = combo,
                                    title = data.title,
                                    year = data.year,
                                    artistNames = data.artistNames,
                                    albumCombo = albumCombo,
                                )
                            }
                        )
                    }
                }
            }
        }
    )
}
