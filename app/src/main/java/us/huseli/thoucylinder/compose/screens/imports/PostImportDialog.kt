package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun PostImportDialog(
    importedAlbumStates: ImmutableCollection<AlbumUiState>,
    notFoundAlbums: ImmutableCollection<String>,
    onGotoAlbumClick: (String) -> Unit,
    onGotoLibraryClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.padding(20.dp),
        shape = MaterialTheme.shapes.extraSmall,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest,
        confirmButton = { SaveButton(text = stringResource(R.string.go_to_library), onClick = onGotoLibraryClick) },
        dismissButton = { CancelButton(text = stringResource(R.string.close), onClick = onDismissRequest) },
        title = { Text(stringResource(R.string.import_finished)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (importedAlbumStates.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.successfully_imported),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        importedAlbumStates.forEach { state ->
                            val albumString =
                                state.artists.joined()?.let { "$it - ${state.title}" } ?: state.title

                            OutlinedButton(
                                onClick = { onGotoAlbumClick(state.albumId) },
                                modifier = Modifier.fillMaxWidth(),
                                content = { Text(text = albumString.umlautify(), modifier = Modifier.fillMaxWidth()) },
                                shape = MaterialTheme.shapes.extraSmall,
                                contentPadding = PaddingValues(10.dp),
                            )
                        }
                    }
                }
                if (notFoundAlbums.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.no_match_found),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        notFoundAlbums.forEach { album ->
                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                content = { Text(text = album.umlautify(), modifier = Modifier.fillMaxWidth()) },
                                shape = MaterialTheme.shapes.extraSmall,
                                contentPadding = PaddingValues(10.dp),
                                enabled = false,
                            )
                        }
                    }
                }
            }
        }
    )
}
