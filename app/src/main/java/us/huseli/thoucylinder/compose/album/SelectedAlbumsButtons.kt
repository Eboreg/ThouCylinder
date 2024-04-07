package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SelectAll
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.AnimatedSection
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedAlbumsButtons(
    albumCount: Int,
    tonalElevation: Dp = 2.dp,
    callbacks: AlbumSelectionCallbacks,
    extraButtons: (@Composable () -> Unit)? = null,
) {
    AnimatedSection(visible = albumCount > 0, tonalElevation = tonalElevation) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Badge(
                    modifier = Modifier.height(32.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    content = { Text(albumCount.toString(), fontWeight = FontWeight.Bold) },
                )
                SmallOutlinedButton(
                    onClick = callbacks.onAddToPlaylistClick,
                    content = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, stringResource(R.string.add_to_playlist)) },
                )
                SmallOutlinedButton(
                    onClick = callbacks.onEnqueueClick,
                    content = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, stringResource(R.string.enqueue)) },
                )
                callbacks.onDeleteClick?.also { onDeleteClick ->
                    SmallOutlinedButton(
                        onClick = onDeleteClick,
                        content = { Icon(Icons.Sharp.Delete, stringResource(R.string.delete)) },
                    )
                }
                SmallOutlinedButton(
                    onClick = callbacks.onPlayClick,
                    content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
                )
                extraButtons?.invoke()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                IconButton(
                    onClick = callbacks.onSelectAllClick,
                    content = { Icon(Icons.Sharp.SelectAll, stringResource(R.string.select_all)) },
                    modifier = Modifier.size(32.dp),
                )
                IconButton(
                    onClick = callbacks.onUnselectAllClick,
                    content = { Icon(Icons.Sharp.Close, stringResource(R.string.unselect_all)) },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
