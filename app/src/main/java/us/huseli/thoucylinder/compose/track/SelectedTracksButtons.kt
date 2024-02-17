package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AnimatedSection
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedTracksButtons(
    trackCount: Int,
    tonalElevation: Dp = 2.dp,
    callbacks: TrackSelectionCallbacks,
    extraButtons: (@Composable () -> Unit)? = null,
) {
    AnimatedSection(visible = trackCount > 0, tonalElevation = tonalElevation) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Badge(
                    modifier = Modifier.height(32.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    content = { Text(trackCount.toString(), fontWeight = FontWeight.Bold) },
                )
                SmallOutlinedButton(
                    onClick = callbacks.onPlayClick,
                    text = stringResource(R.string.play),
                )
                SmallOutlinedButton(
                    onClick = callbacks.onEnqueueClick,
                    text = stringResource(R.string.enqueue),
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
                modifier = Modifier.padding(start = 5.dp).size(32.dp),
            )
        }
    }
}
