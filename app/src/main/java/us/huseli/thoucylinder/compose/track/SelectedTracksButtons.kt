package us.huseli.thoucylinder.compose.track

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.Deselect
import androidx.compose.material.icons.sharp.ImportExport
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.SelectionAction
import us.huseli.thoucylinder.compose.utils.SelectionButtons
import us.huseli.thoucylinder.dataclasses.track.TrackSelectionCallbacks

@Composable
fun SelectedTracksButtons(
    trackCount: () -> Int,
    tonalElevation: Dp = 2.dp,
    callbacks: TrackSelectionCallbacks,
    extraActions: ImmutableList<SelectionAction> = persistentListOf(),
) {
    val actions = listOfNotNull(
        callbacks.onPlayClick?.let {
            SelectionAction(
                onClick = it,
                icon = Icons.Sharp.PlayArrow,
                description = R.string.play,
            )
        },
        callbacks.onEnqueueClick?.let {
            SelectionAction(
                onClick = it,
                icon = Icons.AutoMirrored.Sharp.PlaylistPlay,
                description = R.string.enqueue,
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
        actions = actions.plus(extraActions).toImmutableList(),
        itemCount = trackCount,
        tonalElevation = tonalElevation,
        maxButtonCount = 3,
    )
}
