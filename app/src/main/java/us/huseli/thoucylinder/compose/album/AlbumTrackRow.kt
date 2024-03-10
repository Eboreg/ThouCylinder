package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.track.TrackContextButtonWithMenu
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import kotlin.time.Duration

data class AlbumTrackRowData(
    val title: String,
    val isDownloadable: Boolean,
    val isInLibrary: Boolean,
    val artists: List<TrackArtistCredit>,
    val duration: Duration? = null,
    val downloadTask: TrackDownloadTask? = null,
    val showArtist: Boolean = true,
    val isSelected: Boolean = false,
    val position: String,
    val isPlayable: Boolean,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumTrackRow(
    data: AlbumTrackRowData,
    callbacks: TrackCallbacks<*>,
    modifier: Modifier = Modifier,
    positionColumnWidth: Dp = 40.dp,
) {
    val artistString = data.artists.joined()
    val surfaceColor =
        if (data.isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent
    val textColor =
        if (data.isPlayable) Color.Unspecified
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val textColorSecondary =
        if (data.isPlayable) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier.combinedClickable(
            onClick = { callbacks.onTrackClick?.invoke() },
            onLongClick = callbacks.onLongClick,
        )
    ) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = surfaceColor) {
            Column(modifier = modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = data.position,
                        modifier = Modifier.width(positionColumnWidth),
                        style = ThouCylinderTheme.typographyExtended.listNormalTitle,
                        textAlign = TextAlign.End,
                        color = textColor,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title.umlautify(),
                            maxLines = if (artistString == null || !data.showArtist) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                            color = textColor,
                        )
                        if (data.showArtist && artistString != null) Text(
                            text = artistString,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            color = textColorSecondary,
                        )
                    }
                    data.duration?.also {
                        Text(
                            text = it.sensibleFormat(),
                            style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                            color = textColor,
                        )
                    }

                    TrackContextButtonWithMenu(
                        isDownloadable = data.isDownloadable,
                        callbacks = callbacks,
                        hideAlbum = true,
                        isInLibrary = data.isInLibrary,
                        trackArtists = data.artists,
                    )
                }

                data.downloadTask?.also { downloadTask ->
                    val isActive by downloadTask.isActive.collectAsStateWithLifecycle(false)

                    if (isActive) {
                        val status by downloadTask.downloadStatus.collectAsStateWithLifecycle()
                        val progress by downloadTask.downloadProgress.collectAsStateWithLifecycle()
                        val statusText = stringResource(status.stringId)

                        Row(
                            modifier = Modifier.padding(start = positionColumnWidth + 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$statusText ...",
                                style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                                modifier = Modifier.width(110.dp),
                            )
                            LinearProgressIndicator(
                                progress = { progress.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
