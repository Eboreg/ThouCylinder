package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState
import us.huseli.thoucylinder.dataclasses.artist.LocalArtistCallbacks
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ArtistGrid(
    uiStates: () -> ImmutableList<ArtistUiState>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onEmpty: @Composable () -> Unit = {},
) {
    val callbacks = LocalArtistCallbacks.current

    ItemGrid(
        things = uiStates,
        isLoading = isLoading,
        modifier = modifier,
        onClick = { callbacks.onGotoArtistClick(it.artistId) },
        key = { it.artistId },
        onEmpty = onEmpty,
    ) { state ->
        Thumbnail(
            model = state.thumbnailUri,
            modifier = Modifier.fillMaxWidth(),
            borderWidth = null,
            placeholderIcon = Icons.Sharp.InterpreterMode,
            shape = RectangleShape,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primarySmallBold,
                )
                Text(
                    style = FistopyTheme.bodyStyles.secondarySmall,
                    text = pluralStringResource(R.plurals.x_tracks, state.trackCount, state.trackCount),
                )
            }

            ArtistBottomSheetWithButton(state = state)
        }
    }
}
