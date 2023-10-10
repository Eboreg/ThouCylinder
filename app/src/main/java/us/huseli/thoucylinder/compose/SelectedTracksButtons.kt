package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.compose.SmallOutlinedButton
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedTracksButtons(
    selection: Selection,
    onAddToPlaylistClick: () -> Unit,
    onUnselectAllClick: () -> Unit,
    extraButtons: (@Composable () -> Unit)? = null,
) {
    if (selection.tracks.isNotEmpty() || selection.queueTracks.isNotEmpty()) {
        val trackCount = selection.tracks.size + selection.queueTracks.size

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            Text(
                pluralStringResource(R.plurals.x_selected_tracks, trackCount, trackCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SmallOutlinedButton(
                    onClick = onAddToPlaylistClick,
                    text = stringResource(R.string.add_to_playlist),
                )
                SmallOutlinedButton(
                    onClick = onUnselectAllClick,
                    text = stringResource(R.string.unselect_all),
                )
                extraButtons?.invoke()
            }
        }
    }
}
