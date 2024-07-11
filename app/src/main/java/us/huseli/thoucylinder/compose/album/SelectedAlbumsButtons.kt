package us.huseli.thoucylinder.compose.album

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Deselect
import androidx.compose.material.icons.sharp.ImportExport
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.SelectionAction
import us.huseli.thoucylinder.compose.utils.SelectionButtons
import us.huseli.thoucylinder.dataclasses.album.AlbumSelectionCallbacks

@Composable
fun SelectedAlbumsButtons(
    albumCount: () -> Int,
    tonalElevation: Dp = 2.dp,
    callbacks: AlbumSelectionCallbacks,
) {
    val actions = listOfNotNull(
        SelectionAction(
            onClick = callbacks.onPlayClick,
            icon = Icons.Sharp.PlayArrow,
            showDescriptionOnButton = true,
            description = R.string.play,
        ),
        SelectionAction(
            onClick = callbacks.onEnqueueClick,
            icon = Icons.AutoMirrored.Sharp.PlaylistPlay,
            showDescriptionOnButton = true,
            description = R.string.enqueue,
        ),
        SelectionAction(
            onClick = callbacks.onAddToPlaylistClick,
            icon = Icons.AutoMirrored.Sharp.PlaylistAdd,
            description = R.string.add_to_playlist,
        ),
        callbacks.onDeleteClick?.let {
            SelectionAction(
                onClick = it,
                icon = Icons.Sharp.Delete,
                description = R.string.delete,
            )
        },
        SelectionAction(
            onClick = callbacks.onExportClick,
            icon = Icons.Sharp.ImportExport,
            description = R.string.export_to_playlist_file,
        ),
        SelectionAction(
            onClick = callbacks.onSelectAllClick,
            icon = Icons.Sharp.SelectAll,
            description = R.string.select_all,
        ),
        SelectionAction(
            onClick = callbacks.onUnselectAllClick,
            icon = Icons.Sharp.Deselect,
            description = R.string.unselect_all,
        ),
    )

    SelectionButtons(
        actions = actions.toImmutableList(),
        itemCount = albumCount,
        tonalElevation = tonalElevation,
        maxButtonCount = 2,
    )
}
