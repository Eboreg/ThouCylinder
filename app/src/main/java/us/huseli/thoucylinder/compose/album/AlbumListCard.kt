package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.DownloadStateProgressIndicator
import us.huseli.thoucylinder.compose.utils.ItemListCardWithThumbnail
import us.huseli.thoucylinder.dataclasses.album.IAlbumUiState
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun AlbumListCard(
    state: IAlbumUiState,
    downloadStateFlow: () -> StateFlow<AlbumDownloadTask.UiState?>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtist: Boolean = true,
) {
    val thirdRow = listOfNotNull(
        state.trackCount?.let { pluralStringResource(R.plurals.x_tracks, it, it) },
        state.yearString,
        state.albumType?.let { stringResource(it.stringRes) },
    ).joinToString(" • ").nullIfBlank()
    val downloadState by downloadStateFlow().collectAsStateWithLifecycle()

    ItemListCardWithThumbnail(
        thumbnailModel = state,
        thumbnailPlaceholder = Icons.Sharp.Album,
        isSelected = { state.isSelected },
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        Column {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = state.title.umlautify(),
                        maxLines = if (state.artistString != null && showArtist) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        style = FistopyTheme.bodyStyles.primaryBold,
                    )
                    if (showArtist) state.artistString?.also {
                        Text(
                            text = it.umlautify(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = FistopyTheme.bodyStyles.primarySmall,
                        )
                    }
                    if (thirdRow != null) Text(
                        text = thirdRow,
                        style = FistopyTheme.bodyStyles.secondarySmall,
                        maxLines = 1,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight(),
                    content = { AlbumSmallIcons(isLocal = state.isLocal, isOnYoutube = state.isOnYoutube) },
                )

                AlbumBottomSheetWithButton(uiState = state)
            }

            downloadState?.also { DownloadStateProgressIndicator(it) }
        }
    }
}
