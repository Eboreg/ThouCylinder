package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel

@Composable
fun EditAlbumTracksDialog(
    albumId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    val albumWithTracks by viewModel.flowAlbumWithTracks(albumId).collectAsStateWithLifecycle(null)

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.edit_album_tracks)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        confirmButton = {},
        dismissButton = { CancelButton(onClick = onClose, text = stringResource(R.string.close)) },
        text = {
            albumWithTracks?.also { albumCombo ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(albumCombo.trackCombos) { combo ->
                        val artistNames = combo.artists.map { it.name }.takeIf { it.isNotEmpty() } ?: listOf("")
                        val trackComboString = combo.toString(showYear = true, albumCombo = albumCombo).umlautify()

                        EditIndividualAlbumTrackSection(
                            track = combo.track,
                            trackComboString = trackComboString,
                            artistNames = artistNames.toImmutableList(),
                            enabled = true,
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
