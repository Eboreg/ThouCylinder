package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedAlbumsButtons(
    albumCount: Int,
    tonalElevation: Dp = 2.dp,
    onPlayClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onUnselectAllClick: () -> Unit,
    extraButtons: (@Composable () -> Unit)? = null,
) {
    SelectedItemsActionSection(visible = albumCount > 0, tonalElevation = tonalElevation) {
        Text(
            pluralStringResource(R.plurals.x_selected_albums, albumCount, albumCount),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SmallOutlinedButton(
                onClick = {
                    onAddToPlaylistClick()
                    onUnselectAllClick()
                },
                text = stringResource(R.string.add_to_playlist),
            )
            SmallOutlinedButton(
                onClick = {
                    onPlayClick()
                    onUnselectAllClick()
                },
                text = stringResource(R.string.play),
            )
            extraButtons?.invoke()
            IconButton(
                onClick = onUnselectAllClick,
                content = { Icon(Icons.Sharp.Close, stringResource(R.string.unselect_all)) },
                modifier = Modifier.size(25.dp),
            )
        }
    }
}
