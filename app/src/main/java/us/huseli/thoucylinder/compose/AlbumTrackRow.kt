package us.huseli.thoucylinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumTrackRow(
    track: Track,
    downloadProgress: DownloadProgress?,
    isPlaying: Boolean,
    onToggleSelected: () -> Unit,
    onDownloadClick: () -> Unit,
    onPlayOrPauseClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAlbumClick: ((UUID) -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    showArtist: Boolean = true,
    isSelected: Boolean = false,
    selectOnShortClick: Boolean = false,
) {
    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
        Column(
            modifier = modifier.combinedClickable(
                onClick = { if (selectOnShortClick) onToggleSelected() },
                onLongClick = onToggleSelected,
            ).let { modifier ->
                if (isSelected) {
                    modifier.border(CardDefaults.outlinedCardBorder().let { it.copy(width = it.width + 2.dp) })
                } else modifier
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = track.toString(showArtist = showArtist, showAlbumPosition = true),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.metadata?.let {
                    Text(text = it.duration.sensibleFormat(), modifier = Modifier.padding(start = 10.dp))
                }

                TrackContextMenuWithButton(
                    track = track,
                    metadata = track.metadata,
                    onDownloadClick = onDownloadClick,
                    modifier = Modifier.padding(start = 10.dp).width(30.dp),
                    onGotoArtistClick = onArtistClick,
                    onGotoAlbumClick = onAlbumClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                )

                IconButton(
                    onClick = onPlayOrPauseClick,
                    content = {
                        if (isPlaying) Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                        else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
                    },
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
