package us.huseli.thoucylinder.compose.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState

@Composable
fun ArtistCollection(
    states: () -> ImmutableList<ArtistUiState>,
    displayType: DisplayType,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onEmpty: @Composable () -> Unit = {},
) {
    when (displayType) {
        DisplayType.LIST -> ArtistList(
            states = states,
            modifier = modifier,
            isLoading = isLoading,
            onEmpty = onEmpty,
        )
        DisplayType.GRID -> ArtistGrid(
            uiStates = states,
            modifier = modifier,
            isLoading = isLoading,
            onEmpty = onEmpty,
        )
    }
}
