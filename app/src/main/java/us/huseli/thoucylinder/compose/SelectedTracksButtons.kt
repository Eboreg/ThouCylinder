package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.compose.SmallOutlinedButton
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.SelectedItemsActionSection
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedTracksButtons(
    trackCount: Int,
    tonalElevation: Dp = 2.dp,
    callbacks: TrackSelectionCallbacks,
    extraButtons: (@Composable () -> Unit)? = null,
) {
    SelectedItemsActionSection(visible = trackCount > 0, tonalElevation = tonalElevation) {
        Text(
            pluralStringResource(R.plurals.x_selected_tracks, trackCount, trackCount),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SmallOutlinedButton(
                    onClick = callbacks.onPlayClick,
                    text = stringResource(R.string.play),
                )
                SmallOutlinedButton(
                    onClick = callbacks.onPlayNextClick,
                    text = stringResource(R.string.play_next),
                )
                SmallOutlinedButton(
                    onClick = callbacks.onAddToPlaylistClick,
                    text = stringResource(R.string.add_to_playlist),
                )
                extraButtons?.invoke()
            }
            IconButton(
                onClick = callbacks.onUnselectAllClick,
                content = { Icon(Icons.Sharp.Close, stringResource(R.string.unselect_all)) },
                modifier = Modifier.size(25.dp).padding(start = 5.dp),
            )
        }
    }
}
