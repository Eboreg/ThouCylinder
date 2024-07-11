package us.huseli.thoucylinder.compose.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState
import us.huseli.thoucylinder.dataclasses.artist.LocalArtistCallbacks
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ArtistList(
    states: () -> ImmutableList<ArtistUiState>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onEmpty: @Composable () -> Unit = {},
) {
    val callbacks = LocalArtistCallbacks.current

    ItemList(
        things = states,
        key = { it.artistId },
        modifier = modifier,
        isLoading = isLoading,
        onEmpty = onEmpty,
        contentType = "ArtistUiState",
    ) { state ->
        ItemListCardWithThumbnail(
            thumbnailModel = state.thumbnailUri,
            thumbnailPlaceholder = Icons.Sharp.InterpreterMode,
            onClick = { callbacks.onGotoArtistClick(state.artistId) },
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = state.name.umlautify(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primaryBold,
                )
                Text(
                    style = FistopyTheme.bodyStyles.secondarySmall,
                    text = pluralStringResource(R.plurals.x_albums, state.albumCount, state.albumCount) + " • " +
                        pluralStringResource(R.plurals.x_tracks, state.trackCount, state.trackCount),
                )
            }

            ArtistBottomSheetWithButton(state = state)
        }
    }
}
