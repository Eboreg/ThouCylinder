package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.PlayerRepository
import kotlin.time.DurationUnit

@Composable
fun BottomBar(
    currentTrack: Track,
    playbackState: PlayerRepository.PlaybackState,
    currentPositionMs: Long,
    imageBitmap: ImageBitmap?,
    onPlayOrPauseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val endPosition = currentTrack.metadata?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }

    BottomAppBar {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).padding(vertical = 5.dp).padding(start = 5.dp),
            ) {
                Thumbnail(
                    image = imageBitmap,
                    placeholder = {
                        Image(
                            imageVector = Icons.Sharp.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().aspectRatio(1f),
                        )
                    },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = currentTrack.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    currentTrack.artist?.also { artist ->
                        Text(
                            text = artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Row {
                    if (playbackState == PlayerRepository.PlaybackState.PLAYING) {
                        IconButton(
                            onClick = onPlayOrPauseClick,
                            content = { Icon(Icons.Sharp.Pause, stringResource(R.string.pause)) },
                        )
                    } else {
                        IconButton(
                            onClick = onPlayOrPauseClick,
                            content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
                        )
                    }
                    IconButton(
                        onClick = onNextClick,
                        content = { Icon(Icons.Sharp.SkipNext, stringResource(R.string.next)) },
                    )
                }
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                progress = endPosition?.let { currentPositionMs / it.toFloat() } ?: 0f,
            )
        }
    }
}
