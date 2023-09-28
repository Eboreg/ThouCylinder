package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.themeColors

@Composable
fun TrackInfoDialog(
    track: Track,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
        confirmButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TrackInfoTextRow(label = stringResource(R.string.year), value = track.year?.toString() ?: "-")
                track.metadata?.let { metadata ->
                    TrackInfoTextRow(label = stringResource(R.string.mime_type), value = metadata.mimeType)
                    TrackInfoTextRow(
                        label = stringResource(R.string.file_size),
                        value = metadata.sizeString ?: "-"
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.bit_rate),
                        value = metadata.bitrateString ?: "-"
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.channels),
                        value = metadata.channels?.toString() ?: "-",
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.sample_rate),
                        value = metadata.sampleRateString ?: "",
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.loudness),
                        value = metadata.loudnessDbString ?: "-",
                    )
                }
                TrackInfoBooleanRow(label = stringResource(R.string.is_downloaded), value = track.isDownloaded)
                TrackInfoBooleanRow(label = stringResource(R.string.is_on_youtube), value = track.isOnYoutube)
                track.mediaStoreFile?.path?.let { path ->
                    Text(text = stringResource(R.string.local_path))
                    Text(text = path, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

@Composable
fun TrackInfoBooleanRow(label: String, value: Boolean) {
    val colors = themeColors()
    val iconModifier = Modifier.size(20.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(0.4f))
        if (value) Icon(Icons.Rounded.CheckCircle, null, tint = colors.Green, modifier = iconModifier)
        else Icon(Icons.Rounded.Cancel, null, tint = colors.Red, modifier = iconModifier)
    }
}

@Composable
fun TrackInfoTextRow(label: String, value: String) {
    Row {
        Text(text = label, modifier = Modifier.weight(0.4f))
        Text(text = value)
    }
}
