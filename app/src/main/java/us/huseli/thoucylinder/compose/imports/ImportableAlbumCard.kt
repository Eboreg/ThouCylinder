package us.huseli.thoucylinder.compose.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.album.ImportableAlbumUiState
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun ImportableAlbumCard(
    state: ImportableAlbumUiState,
    backend: ImportBackend,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val albumArtUrl = if (!state.isSaved && state.importError == null) state.thumbnailUrl else null
    val uriHandler = LocalUriHandler.current

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
            Text(
                text = state.title.umlautify(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = FistopyTheme.bodyStyles.primaryBold,
            )
            state.artistString?.also {
                Text(
                    text = it.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primarySmall,
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
                val strings = listOfNotNull(
                    state.trackCount?.let { pluralStringResource(R.plurals.x_tracks, it, it) },
                    state.yearString,
                    state.playCount?.let { stringResource(R.string.play_count_x, it) },
                )

                if (strings.isNotEmpty()) Text(
                    text = strings.joinToString(" • ").umlautify(),
                    style = FistopyTheme.bodyStyles.secondarySmall,
                    maxLines = 1,
                )
            }
        }

        if (backend == ImportBackend.SPOTIFY && state.spotifyWebUrl != null) {
            IconButton(
                onClick = { uriHandler.openUri(state.spotifyWebUrl) },
                content = {
                    Icon(
                        painter = painterResource(R.drawable.spotify),
                        contentDescription = stringResource(R.string.on_spotify),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            )
        }
    }
}
