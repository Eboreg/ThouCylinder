package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun <A : IExternalAlbum> PostImportDialog(
    importedAlbumStates: ImmutableCollection<Album.ViewState>,
    notFoundAlbums: ImmutableCollection<A>,
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
                            style = MaterialTheme.typography.titleLarge,
                        )
                        importedAlbumStates.forEachIndexed { index, state ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val albumString =
                                    state.artists.joined()?.let { "$it - ${state.album.title}" } ?: state.album.title

                                Text(text = albumString.umlautify(), modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { onGotoAlbumClick(state.album.albumId) },
                                    content = { Icon(Icons.Sharp.Album, stringResource(R.string.go_to_album)) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                )
                            }
                            if (index < importedAlbumStates.size - 1) HorizontalDivider()
                        }
                    }
                }
                if (notFoundAlbums.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.no_match_found),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        notFoundAlbums.forEachIndexed { index, album ->
                            val albumString =
                                album.artistName?.takeIf { it.isNotEmpty() }?.let { "$it - ${album.title}" }
                                    ?: album.title

                            Text(text = albumString.umlautify(), modifier = Modifier.padding(vertical = 10.dp))
                            if (index < notFoundAlbums.size - 1) HorizontalDivider()
                        }
                    }
                }
            }
        }
    )
}
