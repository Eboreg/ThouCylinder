package us.huseli.thoucylinder.compose.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.album.ImportableAlbumUiState
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ImportableAlbumCard(state: ImportableAlbumUiState, onClick: () -> Unit, onLongClick: () -> Unit) {
    val albumArtUrl = if (!state.isSaved && state.importError == null) state.thumbnailUri else null

    ItemListCardWithThumbnail(
        thumbnailModel = albumArtUrl,
        thumbnailPlaceholder = when {
            state.isSaved -> Icons.Sharp.CheckCircle
            state.importError != null -> Icons.Sharp.Cancel
            else -> Icons.Sharp.Album
        },
        thumbnailPlaceholderTint = when {
            state.isSaved -> LocalBasicColors.current.Green
            state.importError != null -> LocalBasicColors.current.Red
            else -> null
        },
        isSelected = { state.isSelected },
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val textColor =
                if (state.isSaved || state.importError != null) MaterialTheme.colorScheme.onSurfaceVariant
                else Color.Unspecified

            Text(
                text = state.title.umlautify(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = FistopyTheme.bodyStyles.primaryBold,
                color = textColor,
            )
            state.artistString?.also {
                Text(
                    text = it.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primarySmall,
                    color = textColor,
                )
            }
            if (state.isSaved) {
                Badge(
                    containerColor = LocalBasicColors.current.Green,
                    content = { Text(text = stringResource(R.string.imported)) },
                )
            } else if (state.importError != null) {
                Badge(
                    containerColor = LocalBasicColors.current.Red,
                    content = { Text(text = stringResource(R.string.no_match_found)) },
                )
            } else {
                val strings = mutableListOf<String>()

                state.trackCount?.also { strings.add(pluralStringResource(R.plurals.x_tracks, it, it)) }
                state.yearString?.also { strings.add(it) }
                state.playCount?.also { strings.add(stringResource(R.string.play_count_x, it)) }

                if (strings.isNotEmpty()) Text(
                    text = strings.joinToString(" • ").umlautify(),
                    style = FistopyTheme.bodyStyles.secondarySmall,
                    maxLines = 1,
                )
            }
        }
    }
}
