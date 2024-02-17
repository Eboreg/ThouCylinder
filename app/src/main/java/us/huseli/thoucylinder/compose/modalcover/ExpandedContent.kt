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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.viewmodels.QueueViewModel

@Composable
fun ExpandedContent(
    viewModel: QueueViewModel,
    endPositionMs: Float,
    canPlay: Boolean,
    canGotoNext: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val toggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContentColor = LocalContentColor.current,
        contentColor = LocalContentColor.current.copy(alpha = 0.5f),
    )

    Column(
        modifier = modifier.padding(top = 15.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        ExpandedProgressBar(
            viewModel = viewModel,
            containerColor = containerColor,
            contentColor = contentColor,
            endPositionMs = endPositionMs,
        )

        // Large button row:
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconToggleButton(
                checked = isShuffleEnabled,
                onCheckedChange = { viewModel.toggleShuffle() },
                content = { Icon(Icons.Sharp.Shuffle, null) },
                colors = toggleButtonColors,
            )
            IconButton(
                onClick = { viewModel.skipToStartOrPrevious() },
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
                onClick = { viewModel.playOrPauseCurrent() },
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
            IconButton(
                onClick = { viewModel.skipToNext() },
                content = {
                    Icon(
                        imageVector = Icons.Sharp.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        modifier = Modifier.scale(1.5f),
                    )
                },
                enabled = canGotoNext,
                modifier = Modifier.size(60.dp),
            )
            IconToggleButton(
                checked = isRepeatEnabled,
                onCheckedChange = { viewModel.toggleRepeat() },
                content = { Icon(Icons.Sharp.Repeat, null) },
                colors = toggleButtonColors,
            )
        }
    }
}