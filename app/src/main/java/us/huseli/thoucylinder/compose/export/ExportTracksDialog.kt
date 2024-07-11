package us.huseli.thoucylinder.compose.export

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.ProgressSection
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.ExportViewModel

@Composable
fun ExportTracksDialog(
    trackCount: Int,
    progress: ProgressData,
    onDismissRequest: () -> Unit,
    onExportXspfClick: () -> Unit,
    onExportJspfClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        shape = MaterialTheme.shapes.small,
        dismissButton = { CancelButton(onClick = onDismissRequest) },
        title = { Text(stringResource(R.string.export_to_playlist_file)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    pluralStringResource(
                        R.plurals.x_tracks_will_be_exported_to_an_xspf_or_jspf_file,
                        trackCount,
                        trackCount,
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = onExportXspfClick,
                        content = { Text("XSPF") },
                        shape = MaterialTheme.shapes.small,
                        enabled = !progress.isActive,
                    )
                    FilledTonalButton(
                        onClick = onExportJspfClick,
                        content = { Text("JSPF") },
                        shape = MaterialTheme.shapes.small,
                        enabled = !progress.isActive,
                    )
                }
                ProgressSection(progress = progress)
            }
        },
    )
}

@Composable
fun ExportTracksDialog(
    trackIds: ImmutableList<String>? = null,
    albumIds: ImmutableList<String>? = null,
    playlistId: String? = null,
    viewModel: ExportViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val trackCombos by viewModel.trackCombos.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val trackCount = remember(trackCombos) { trackCombos.size }
    val exportDateTime = remember(trackCombos) { viewModel.getDateTime() }

    val xspfLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType = "application/xspf+xml")) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                viewModel.exportTracksAsXspf(
                    trackCombos = trackCombos,
                    outputUri = uri,
                    dateTime = exportDateTime,
                    onFinish = onClose,
                )
            }
        }

    val jspfLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType = "application/json")) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                viewModel.exportTracksAsJspf(
                    trackCombos = trackCombos,
                    outputUri = uri,
                    dateTime = exportDateTime,
                    onFinish = onClose,
                )
            }
        }

    LaunchedEffect(trackIds, albumIds, playlistId) {
        if (trackIds?.isNotEmpty() == true) viewModel.setTrackIds(trackIds)
        else if (albumIds?.isNotEmpty() == true) viewModel.setAlbumIds(albumIds)
        else if (playlistId != null) viewModel.setPlaylistId(playlistId)
    }

    if (trackCount > 0) {
        ExportTracksDialog(
            trackCount = trackCount,
            progress = progress,
            onDismissRequest = {
                viewModel.clear()
                onClose()
            },
            onExportXspfClick = { xspfLauncher.launch(viewModel.getXspfFilename(exportDateTime)) },
            onExportJspfClick = { jspfLauncher.launch(viewModel.getJspfFilename(exportDateTime)) },
        )
    }
}
