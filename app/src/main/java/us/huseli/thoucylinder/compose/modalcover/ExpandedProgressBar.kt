package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun ExpandedProgressBar(
    viewModel: QueueViewModel,
    endPositionMs: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    var currentPositionSeconds by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    val sliderInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(Unit) {
        sliderInteractionSource.interactions.collect { interaction ->
            isDragging = interaction is DragInteraction.Start
        }
    }

    LaunchedEffect(Unit) {
        viewModel.currentProgress.collect { progress ->
            if (!isDragging) currentProgress = progress
        }
    }

    LaunchedEffect(Unit) {
        viewModel.currentPositionSeconds.collect { pos ->
            if (!isDragging) currentPositionSeconds = pos
        }
    }

    Slider(
        value = currentProgress,
        interactionSource = sliderInteractionSource,
        modifier = modifier.padding(horizontal = 10.dp).height(30.dp),
        colors = SliderDefaults.colors(
            thumbColor = contentColor,
            activeTrackColor = contentColor,
            inactiveTrackColor = containerColor,
        ),
        onValueChange = {
            currentProgress = it
            currentPositionSeconds = ((endPositionMs / 1000) * it).toLong()
        },
        onValueChangeFinished = { viewModel.seekToProgress(currentProgress) },
    )
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = currentPositionSeconds.seconds.sensibleFormat(),
            style = ThouCylinderTheme.typographyExtended.listSmallTitle,
        )
        Text(
            text = endPositionMs.toDouble().milliseconds.sensibleFormat(),
            style = ThouCylinderTheme.typographyExtended.listSmallTitle,
        )
    }
}