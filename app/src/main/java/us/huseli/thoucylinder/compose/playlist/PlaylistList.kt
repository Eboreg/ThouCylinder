package us.huseli.thoucylinder.compose.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.dataclasses.playlist.PlaylistUiState
import us.huseli.thoucylinder.umlautify

@Composable
fun PlaylistList(
    uiStates: () -> ImmutableList<PlaylistUiState>,
    isLoading: Boolean,
    contextMenu: @Composable (PlaylistUiState) -> Unit,
    modifier: Modifier = Modifier,
    onEmpty: @Composable () -> Unit = {},
) {
    val appCallbacks = LocalAppCallbacks.current

    ItemList(
        things = uiStates,
        key = { it.id },
        isLoading = isLoading,
        modifier = modifier,
        onEmpty = onEmpty,
        contentType = "PlaylistUiState",
    ) { state ->
        ItemListCardWithThumbnail(
            thumbnailModel = state.thumbnailUris,
            thumbnailPlaceholder = Icons.AutoMirrored.Sharp.QueueMusic,
            height = 60.dp,
            onClick = { appCallbacks.onGotoPlaylistClick(state.id) },
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = state.name.umlautify(),
                    maxLines = 2,
                    style = FistopyTheme.bodyStyles.primaryBold,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.getSecondRow(),
                    style = FistopyTheme.bodyStyles.secondarySmall,
                )
            }

            contextMenu(state)
        }
    }
}
