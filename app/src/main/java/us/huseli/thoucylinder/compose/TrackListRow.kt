package us.huseli.thoucylinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.themeColors
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListRow(
    track: Track,
    metadata: TrackMetadata?,
    showArtist: Boolean,
    isPlaying: Boolean,
    onDownloadClick: () -> Unit,
    onPlayOrPauseClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onToggleSelected: () -> Unit,
    thumbnail: ImageBitmap?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectOnShortClick: Boolean = false,
    downloadProgress: DownloadProgress? = null,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.outlinedCardColors(),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.fillMaxWidth().height(60.dp).combinedClickable(
            onClick = { if (selectOnShortClick) onToggleSelected() },
            onLongClick = onToggleSelected,
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
    ) {
        Row {
            if (isSelected) {
                Box(modifier = Modifier.fillMaxHeight().padding(end = 10.dp).aspectRatio(1f)) {
                    Image(
                        imageVector = Icons.Sharp.CheckCircle,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color = themeColors().Green),
                        modifier = Modifier.fillMaxSize().aspectRatio(1f).padding(5.dp),
                    )
                }
            } else {
                Thumbnail(
                    image = thumbnail,
                    modifier = Modifier.fillMaxHeight().padding(end = 10.dp),
                    placeholder = {
                        Image(
                            imageVector = Icons.Sharp.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().aspectRatio(1f),
                        )
                    },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight().padding(vertical = 5.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = track.toString(showAlbumPosition = false, showArtist = showArtist),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                metadata?.let {
                    Text(text = it.duration.sensibleFormat(), modifier = Modifier.padding(start = 5.dp))
                }

                TrackContextMenuWithButton(
                    track = track,
                    metadata = metadata,
                    onDownloadClick = onDownloadClick,
                    modifier = Modifier.padding(start = 10.dp).width(30.dp),
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
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
        }

        if (downloadProgress != null) {
            val statusText = stringResource(downloadProgress.status.stringId)

            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                Text(text = "$statusText …")
                LinearProgressIndicator(
                    progress = downloadProgress.progress.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
