package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.track.TrackContextMenuWithButton
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import kotlin.time.Duration

data class AlbumTrackRowData(
    val title: String,
    val isDownloadable: Boolean,
    val artist: String? = null,
    val duration: Duration? = null,
    val albumPosition: Int? = null,
    val discNumber: Int? = null,
    val downloadTask: TrackDownloadTask? = null,
    val showArtist: Boolean = true,
    val showDiscNumber: Boolean = false,
    val isSelected: Boolean = false,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumTrackRow(
    data: AlbumTrackRowData,
    callbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
) {
    val surfaceColor =
        if (data.isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent

    Column(
        modifier = modifier.combinedClickable(
            onClick = { callbacks.onTrackClick?.invoke() },
            onLongClick = callbacks.onLongClick,
        )
    ) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = surfaceColor) {
            Column(modifier = modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val position =
                        if (data.showDiscNumber && data.discNumber != null && data.albumPosition != null)
                            "${data.discNumber}.${data.albumPosition}"
                        else data.albumPosition?.toString() ?: ""
                    Text(
                        text = position,
                        modifier = Modifier.width(40.dp),
                        style = ThouCylinderTheme.typographyExtended.listNormalTitle,
                        textAlign = TextAlign.Center,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title,
                            maxLines = if (data.artist == null || !data.showArtist) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                        )
                        if (data.artist != null && data.showArtist) {
                            Text(
                                text = data.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                            )
                        }
                    }
                    data.duration?.also {
                        Text(
                            text = it.sensibleFormat(),
                            modifier = Modifier.padding(start = 10.dp),
                            style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                        )
                    }

                    TrackContextMenuWithButton(
                        isDownloadable = data.isDownloadable,
                        callbacks = callbacks,
                        modifier = Modifier.padding(start = 10.dp).size(30.dp),
                        hideAlbum = true,
                    )
                }

                data.downloadTask?.also { downloadTask ->
                    val isActive by downloadTask.isActive.collectAsStateWithLifecycle(false)

                    if (isActive) {
                        val status by downloadTask.downloadStatus.collectAsStateWithLifecycle()
                        val progress by downloadTask.downloadProgress.collectAsStateWithLifecycle()
                        val statusText = stringResource(status.stringId)

                        Row(
                            modifier = Modifier.padding(start = 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$statusText ...",
                                style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                                modifier = Modifier.width(100.dp),
                            )
                            LinearProgressIndicator(
                                progress = progress.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
