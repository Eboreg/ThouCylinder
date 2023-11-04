package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.TrackMetadata

@Composable
fun TrackInfoDialog(
    isDownloaded: Boolean,
    isOnYoutube: Boolean,
    metadata: TrackMetadata?,
    modifier: Modifier = Modifier,
    albumTitle: String? = null,
    albumArtist: String? = null,
    year: Int? = null,
    onClose: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        onDismissRequest = onClose,
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
        confirmButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TrackInfoTextRow(label = stringResource(R.string.year), value = year?.toString() ?: "-")
                if (albumTitle != null)
                    TrackInfoTextRow(label = stringResource(R.string.album), value = albumTitle)
                if (albumArtist != null)
                    TrackInfoTextRow(label = stringResource(R.string.album_artist), value = albumArtist)
                metadata?.let { metadata ->
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
                TrackInfoBooleanRow(label = stringResource(R.string.is_downloaded), value = isDownloaded)
                TrackInfoBooleanRow(label = stringResource(R.string.is_on_youtube), value = isOnYoutube)
            }
        }
    )
}

@Composable
fun TrackInfoBooleanRow(label: String, value: Boolean) {
    val colors = LocalBasicColors.current
    val iconModifier = Modifier.size(20.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(0.4f))
        if (value) Icon(Icons.Rounded.CheckCircle, null, tint = colors.Green, modifier = iconModifier)
        else Icon(Icons.Rounded.Cancel, null, tint = colors.Red, modifier = iconModifier)
    }
}

@Composable
fun TrackInfoTextRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, modifier = Modifier.weight(0.4f))
        Text(text = value, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}
