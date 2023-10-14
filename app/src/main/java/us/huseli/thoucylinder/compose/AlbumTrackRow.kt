package us.huseli.thoucylinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumTrackRow(
    track: Track,
    album: Album,
    downloadProgress: DownloadProgress?,
    onToggleSelected: () -> Unit,
    onDownloadClick: () -> Unit,
    onPlayClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAlbumClick: ((UUID) -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    showArtist: Boolean = true,
    showDiscNumber: Boolean = false,
    isSelected: Boolean = false,
    selectOnShortClick: Boolean = false,
    onEnqueueNextClick: () -> Unit,
) {
    val surfaceColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent

    Column(
        modifier = modifier.combinedClickable(
            onClick = { if (selectOnShortClick) onToggleSelected() else onPlayClick() },
            onLongClick = onToggleSelected,
        )
    ) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = surfaceColor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val position =
                    if (showDiscNumber && track.discNumber != null && track.albumPosition != null)
                        "${track.discNumber}.${track.albumPosition}"
                    else track.albumPosition?.toString() ?: ""
                Text(
                    text = position,
                    modifier = Modifier.width(40.dp),
                    style = ThouCylinderTheme.typographyExtended.listNormalTitle,
                    textAlign = TextAlign.Center,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        maxLines = if (track.artist == null || !showArtist) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                    )
                    if (track.artist != null && showArtist) {
                        Text(
                            text = track.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                        )
                    }
                }
                track.metadata?.let {
                    Text(
                        text = it.duration.sensibleFormat(),
                        modifier = Modifier.padding(start = 10.dp),
                        style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                    )
                }

                TrackContextMenuWithButton(
                    track = track,
                    album = album,
                    metadata = track.metadata,
                    onDownloadClick = onDownloadClick,
                    modifier = Modifier.padding(start = 10.dp).width(30.dp),
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onEnqueueNextClick = onEnqueueNextClick,
                )
            }

            downloadProgress?.let { progress ->
                val statusText = stringResource(progress.status.stringId)

                Column(modifier = Modifier.padding(bottom = 5.dp)) {
                    Text(text = "$statusText …")
                    LinearProgressIndicator(
                        progress = progress.progress.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
