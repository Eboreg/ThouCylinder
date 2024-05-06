package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import us.huseli.retaintheme.extensions.bytesToString
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackInfoDialog(
    track: Track,
    modifier: Modifier = Modifier,
    albumTitle: String? = null,
    albumArtist: String? = null,
    year: Int? = null,
    localPath: String? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val fileSize = remember { track.getFileSize(context) }

    AlertDialog(
        modifier = modifier.padding(horizontal = 20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        dismissButton = { CancelButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
        confirmButton = {},
        text = {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                maxItemsInEachRow = if (isInLandscapeMode()) 2 else 1,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val rowModifier = Modifier.weight(1f)

                if (albumTitle != null)
                    TrackInfoTextRow(
                        label = stringResource(R.string.album),
                        value = albumTitle.umlautify(),
                        modifier = rowModifier,
                    )
                if (albumArtist != null)
                    TrackInfoTextRow(
                        label = stringResource(R.string.album_artist),
                        value = albumArtist.umlautify(),
                        modifier = rowModifier,
                    )
                TrackInfoTextRow(
                    label = stringResource(R.string.year),
                    value = year?.toString() ?: "-",
                    modifier = rowModifier,
                )
                track.duration?.also { duration ->
                    TrackInfoTextRow(
                        label = stringResource(R.string.duration),
                        value = duration.sensibleFormat(),
                        modifier = rowModifier,
                    )
                }
                track.metadata?.also { metadata ->
                    TrackInfoTextRow(
                        label = stringResource(R.string.mime_type),
                        value = metadata.mimeType,
                        modifier = rowModifier,
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.file_size),
                        value = fileSize?.bytesToString() ?: "-",
                        modifier = rowModifier,
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.bit_rate),
                        value = metadata.bitrateString ?: "-",
                        modifier = rowModifier,
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.channels),
                        value = metadata.channels?.toString() ?: "-",
                        modifier = rowModifier,
                    )
                    TrackInfoTextRow(
                        label = stringResource(R.string.sample_rate),
                        value = metadata.sampleRateString ?: "",
                        modifier = rowModifier,
                    )
                }
                TrackInfoBooleanRow(
                    label = stringResource(R.string.is_in_library),
                    value = track.isInLibrary,
                    modifier = rowModifier,
                )
                TrackInfoBooleanRow(
                    label = stringResource(R.string.is_downloaded),
                    value = track.isDownloaded,
                    modifier = rowModifier,
                )
                TrackInfoBooleanRow(
                    label = stringResource(R.string.is_on_youtube),
                    value = track.isOnYoutube,
                    modifier = rowModifier,
                )
                TrackInfoBooleanRow(
                    label = stringResource(R.string.is_on_spotify),
                    value = track.isOnSpotify,
                    modifier = rowModifier,
                )
                localPath?.also {
                    Text(text = stringResource(R.string.local_file))
                    Text(text = it, style = ThouCylinderTheme.typographyExtended.listSmallTitle)
                }
            }
        }
    )
}

@Composable
fun TrackInfoBooleanRow(label: String, value: Boolean?, modifier: Modifier = Modifier) {
    val colors = LocalBasicColors.current
    val iconModifier = Modifier.size(20.dp)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(text = label.umlautify(), modifier = Modifier.weight(0.4f))
        when (value) {
            true -> Icon(Icons.Rounded.CheckCircle, null, tint = colors.Green, modifier = iconModifier)
            false -> Icon(Icons.Rounded.Cancel, null, tint = colors.Red, modifier = iconModifier)
            null -> Text(text = "-", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TrackInfoTextRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label.umlautify(), modifier = Modifier.weight(0.4f))
        Text(text = value.umlautify(), modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
    }
}
