package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.ModalCoverBooleans
import us.huseli.thoucylinder.dataclasses.callbacks.PlaybackCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun ExpandedPlayButton(
    isLoading: Boolean,
    isPlaying: Boolean,
    canPlay: Boolean,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    FilledTonalIconButton(
        onClick = onClick,
        content = {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                val description =
                    if (isPlaying) stringResource(R.string.pause)
                    else stringResource(R.string.play)
                Icon(
                    imageVector = if (isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                    contentDescription = description,
                    modifier = Modifier.scale(1.75f),
                )
            }
        },
        enabled = canPlay,
        modifier = Modifier.size(80.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    )
}

@Composable
fun ExpandedContent(
    uiBooleans: ModalCoverBooleans,
    endPositionMs: Float,
    maxWidthPx: Float,
    progress: FloatState,
    backgroundColor: Color,
    callbacks: PlaybackCallbacks,
    modifier: Modifier = Modifier,
    amplitudes: State<List<Int>>,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    waveFormHeight: Dp = 60.dp,
) {
    val toggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContentColor = LocalContentColor.current,
        contentColor = LocalContentColor.current.copy(alpha = 0.5f),
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpandedWaveForm(
                amplitudes = amplitudes,
                containerColor = containerColor,
                contentColor = contentColor,
                maxWidthPx = maxWidthPx,
                endPositionMs = endPositionMs,
                backgroundColor = backgroundColor,
                progress = progress,
                seekToProgress = callbacks.seekToProgress,
                height = waveFormHeight,
            )
        }

        // Large button row:
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconToggleButton(
                checked = uiBooleans.isShuffleEnabled,
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
            ExpandedPlayButton(
                isLoading = uiBooleans.isLoading,
                isPlaying = uiBooleans.isPlaying,
                canPlay = uiBooleans.canPlay,
                onClick = callbacks.playOrPauseCurrent,
                contentColor = contentColor,
                containerColor = containerColor,
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
                enabled = uiBooleans.canGotoNext,
                modifier = Modifier.size(60.dp),
            )
            IconToggleButton(
                checked = uiBooleans.isRepeatEnabled,
                onCheckedChange = { callbacks.toggleRepeat() },
                content = { Icon(Icons.Sharp.Repeat, null) },
                colors = toggleButtonColors,
            )
        }
    }
}
