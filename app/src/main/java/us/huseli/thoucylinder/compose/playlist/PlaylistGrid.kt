package us.huseli.thoucylinder.compose.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.compose.utils.Thumbnail4x4
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.dataclasses.playlist.PlaylistUiState
import us.huseli.thoucylinder.umlautify

@Composable
fun PlaylistGrid(
    uiStates: () -> ImmutableList<PlaylistUiState>,
    isLoading: Boolean,
    contextMenu: @Composable (PlaylistUiState) -> Unit,
    modifier: Modifier = Modifier,
    onEmpty: @Composable () -> Unit = {},
) {
    val appCallbacks = LocalAppCallbacks.current

    ItemGrid(
        modifier = modifier,
        isLoading = isLoading,
        things = uiStates,
        key = { it.id },
        onClick = { appCallbacks.onGotoPlaylistClick(it.id) },
        onEmpty = onEmpty,
    ) { state ->
        if (state.thumbnailUris.size >= 4) Thumbnail4x4(
            models = state.thumbnailUris,
            placeholderIcon = Icons.AutoMirrored.Sharp.QueueMusic,
            borderWidth = null,
            shape = RectangleShape,
        ) else Thumbnail(
            model = state.thumbnailUris.firstOrNull(),
            placeholderIcon = Icons.AutoMirrored.Sharp.QueueMusic,
            borderWidth = null,
            shape = RectangleShape,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .height(56.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = state.name.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primaryBold,
                )
                Text(text = state.getSecondRow(), style = FistopyTheme.bodyStyles.primarySmall)
            }

            contextMenu(state)
        }
    }
}
