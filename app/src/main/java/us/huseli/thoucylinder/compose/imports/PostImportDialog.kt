package us.huseli.thoucylinder.compose.imports

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.managers.ExternalContentManager
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun PostImportDialog(
    data: List<ExternalContentManager.AlbumImportData>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val albumCallbacks = LocalAlbumCallbacks.current
    val appCallbacks = LocalAppCallbacks.current
    val failedImports = remember(data) { data.filter { it.error != null } }
    val successfulImports = remember(data) { data.filter { it.error == null } }

    AlertDialog(
        modifier = modifier.padding(20.dp),
        shape = MaterialTheme.shapes.extraSmall,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            SaveButton(
                text = stringResource(R.string.go_to_library),
                onClick = {
                    appCallbacks.onGotoLibraryClick()
                    onDismissRequest()
                },
            )
        },
        dismissButton = { CancelButton(text = stringResource(R.string.close), onClick = onDismissRequest) },
        title = { Text(stringResource(R.string.import_finished)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (successfulImports.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.successfully_imported),
                            style = FistopyTheme.typography.titleMedium,
                        )
                        for (importData in successfulImports) {
                            val albumString =
                                importData.state.artistString?.let { "$it - ${importData.state.title}" }
                                    ?: importData.state.title

                            OutlinedButton(
                                onClick = {
                                    albumCallbacks.onGotoAlbumClick(importData.state.albumId)
                                    onDismissRequest()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                content = { Text(text = albumString.umlautify(), modifier = Modifier.fillMaxWidth()) },
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(10.dp),
                            )
                        }
                    }
                }
                if (failedImports.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.not_imported),
                            style = FistopyTheme.typography.titleMedium,
                        )
                        for (importData in failedImports) {
                            val albumString = importData.state.artistString
                                ?.let { "$it - ${importData.state.title}" }
                                ?: importData.state.title

                            OutlinedButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                content = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(text = albumString.umlautify())
                                        importData.error?.also { error ->
                                            Text(text = error, fontStyle = FontStyle.Italic)
                                        }
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
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
