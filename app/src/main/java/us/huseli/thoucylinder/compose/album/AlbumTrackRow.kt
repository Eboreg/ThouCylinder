package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.dataclasses.uistates.AlbumTrackUiState
import us.huseli.thoucylinder.compose.track.TrackContextButtonWithMenu
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumTrackRow(
    state: AlbumTrackUiState,
    position: String,
    callbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    positionColumnWidth: Dp = 40.dp,
    showArtist: Boolean = true,
    isSelected: Boolean = false,
) {
    val downloadState by state.downloadState.collectAsStateWithLifecycle()
    val textColor =
        if (state.isPlayable) Color.Unspecified
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val textColorSecondary =
        if (state.isPlayable) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 10.dp)
            .combinedClickable(
                onClick = { callbacks.onTrackClick(state.trackId) },
                onLongClick = { callbacks.onLongClick(state.trackId) },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Text(
                text = position,
                modifier = Modifier.width(positionColumnWidth),
                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                textAlign = TextAlign.End,
                color = textColor,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.umlautify(),
                    maxLines = if (state.trackArtistString == null || !showArtist) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                    color = textColor,
                )
                if (showArtist) {
                    state.trackArtistString?.also { artistString ->
                        Text(
                            text = artistString,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            color = textColorSecondary,
                        )
                    }
                }
            }
            state.duration?.also {
                Text(
                    text = it.sensibleFormat(),
                    style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                    color = textColor,
                )
            }

            TrackContextButtonWithMenu(
                trackId = state.trackId,
                isDownloadable = state.isDownloadable,
                callbacks = callbacks,
                hideAlbum = true,
                isInLibrary = state.isInLibrary,
                artists = state.trackArtists,
                youtubeWebUrl = state.youtubeWebUrl,
                spotifyWebUrl = state.spotifyWebUrl,
                isPlayable = state.isPlayable,
            )
        }

        downloadState?.also {
            if (it.isActive) {
                val statusText = stringResource(it.status.stringId)

                Row(
                    modifier = Modifier.padding(start = positionColumnWidth + 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$statusText ...",
                        style = ThouCylinderTheme.typographyExtended.listExtraSmallTitle,
                        modifier = Modifier.width(130.dp),
                    )
                    LinearProgressIndicator(
                        progress = { it.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
