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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.track.TrackBottomSheetWithButton
import us.huseli.thoucylinder.compose.utils.DownloadStateProgressIndicator
import us.huseli.thoucylinder.dataclasses.track.AlbumTrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumTrackRow(
    state: AlbumTrackUiState,
    downloadState: State<TrackDownloadTask.UiState?>,
    position: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    positionColumnWidth: Dp = 40.dp,
    showArtist: Boolean = true,
    containerColor: Color = Color.Transparent,
) {
    val textColor =
        if (state.isPlayable) Color.Unspecified
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val textColorSecondary =
        if (state.isPlayable) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (state.isSelected) MaterialTheme.colorScheme.primaryContainer else containerColor,
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 55.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
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
                style = FistopyTheme.bodyStyles.secondarySmall,
                textAlign = TextAlign.End,
                color = textColor,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.umlautify(),
                    maxLines = if (state.artistString == null || !showArtist) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primaryBold,
                    color = textColor,
                )
                if (showArtist) {
                    state.artistString?.also { artistString ->
                        Text(
                            text = artistString,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = FistopyTheme.bodyStyles.secondarySmall,
                            color = textColorSecondary,
                        )
                    }
                }
            }
            state.durationMs?.also {
                Text(
                    text = it.milliseconds.sensibleFormat(),
                    style = FistopyTheme.bodyStyles.primarySmall,
                    color = textColor,
                )
            }

            TrackBottomSheetWithButton(state = state, hideAlbum = true)
        }

        downloadState.value?.also {
            if (it.isActive) {
                val statusText = stringResource(it.status.stringId)

                Row(
                    modifier = Modifier.padding(start = positionColumnWidth + 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$statusText ...",
                        style = FistopyTheme.bodyStyles.primaryExtraSmall,
                        modifier = Modifier.width(130.dp),
                    )
                    DownloadStateProgressIndicator(it)
                }
            }
        }
    }
}
