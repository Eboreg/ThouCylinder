package us.huseli.thoucylinder.compose.playlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.dataclasses.playlist.PlaylistUiState

@Composable
fun PlaylistCollection(
    displayType: DisplayType,
    uiStates: () -> ImmutableList<PlaylistUiState>,
    isLoading: Boolean,
    contextMenu: @Composable (PlaylistUiState) -> Unit,
    modifier: Modifier = Modifier,
    onEmpty: @Composable () -> Unit = {},
) {
    when (displayType) {
        DisplayType.LIST -> PlaylistList(
            uiStates = uiStates,
            isLoading = isLoading,
            modifier = modifier,
            contextMenu = contextMenu,
            onEmpty = onEmpty,
        )
        DisplayType.GRID -> PlaylistGrid(
            uiStates = uiStates,
            isLoading = isLoading,
            contextMenu = contextMenu,
            modifier = modifier,
            onEmpty = onEmpty,
        )
    }
}
