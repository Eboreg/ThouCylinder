package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.callbacks.PlaybackCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun ExpandedContent(
    endPositionMs: Float,
    canPlay: State<Boolean>,
    canGotoNext: State<Boolean>,
    isPlaying: State<Boolean>,
    isLoading: State<Boolean>,
    isRepeatEnabled: State<Boolean>,
    isShuffleEnabled: State<Boolean>,
    maxWidthPx: Float,
    progress: State<Float>,
    backgroundColor: () -> Color,
    callbacks: PlaybackCallbacks,
    modifier: Modifier = Modifier,
    amplitudes: State<ImmutableList<Int>>,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val toggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContentColor = LocalContentColor.current,
        contentColor = LocalContentColor.current.copy(alpha = 0.5f),
    )

    Column(
        modifier = modifier.padding(top = 15.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ExpandedWaveForm(
            amplitudes = amplitudes.value,
            containerColor = { containerColor },
            contentColor = { contentColor },
            maxWidthPx = maxWidthPx,
            endPositionMs = endPositionMs,
            backgroundColor = backgroundColor,
            progress = progress,
            seekToProgress = callbacks.seekToProgress,
        )

        // Large button row:
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconToggleButton(
                checked = isShuffleEnabled.value,
                onCheckedChange = { callbacks.toggleShuffle() },
                content = { Icon(Icons.Sharp.Shuffle, null) },
                colors = toggleButtonColors,
            )
            IconButton(
                onClick = callbacks.skipToStartOrPrevious,
                content = {
                    Icon(
                        imageVector = Icons.Sharp.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.scale(1.5f),
                    )
                },
                modifier = Modifier.size(60.dp),
            )
            FilledTonalIconButton(
                onClick = callbacks.playOrPauseCurrent,
                content = {
                    if (isLoading.value) {
                        CircularProgressIndicator(
                            color = contentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        val description =
                            if (isPlaying.value) stringResource(R.string.pause)
                            else stringResource(R.string.play)
                        Icon(
                            imageVector = if (isPlaying.value) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                            contentDescription = description,
                            modifier = Modifier.scale(1.75f),
                        )
                    }
                },
                enabled = canPlay.value,
                modifier = Modifier.size(80.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
            )
            IconButton(
                onClick = callbacks.skipToNext,
                content = {
                    Icon(
                        imageVector = Icons.Sharp.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        modifier = Modifier.scale(1.5f),
                    )
                },
                enabled = canGotoNext.value,
                modifier = Modifier.size(60.dp),
            )
            IconToggleButton(
                checked = isRepeatEnabled.value,
                onCheckedChange = { callbacks.toggleRepeat() },
                content = { Icon(Icons.Sharp.Repeat, null) },
                colors = toggleButtonColors,
            )
        }
    }
}
