package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.linc.audiowaveform.AudioWaveform
import com.linc.audiowaveform.model.AmplitudeType
import com.linc.audiowaveform.model.WaveformAlignment
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.compose.FistopyTheme
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun WaveForm(
    amplitudes: State<List<Int>>,
    currentProgress: FloatState,
    containerColor: Color,
    contentColor: Color,
    maxWidthPx: Float,
    height: Dp = 60.dp,
) {
    val density = LocalDensity.current
    val width = remember(maxWidthPx) { maxWidthPx * 2 }
    val widthDp = remember(width) { with(density) { width.toDp() } }
    val offsetX = remember(maxWidthPx) { ((maxWidthPx - with(density) { 40.dp.toPx() }) / 2).toInt() }

    var intOffset by remember { mutableStateOf(IntOffset.Zero) }

    LaunchedEffect(currentProgress, maxWidthPx) {
        snapshotFlow { currentProgress.floatValue }.collect { progress ->
            intOffset = IntOffset(x = offsetX + (-width * progress).roundToInt(), y = 0)
        }
    }

    AudioWaveform(
        amplitudes = amplitudes.value,
        onProgressChange = {},
        style = Fill,
        waveformAlignment = WaveformAlignment.Center,
        amplitudeType = AmplitudeType.Avg,
        progress = currentProgress.floatValue,
        waveformBrush = SolidColor(containerColor),
        progressBrush = SolidColor(contentColor),
        modifier = Modifier
            .width(widthDp)
            .absoluteOffset { intOffset }
            .height(height)
            .requiredHeight(height)
    )
}

@Composable
fun ExpandedWaveForm(
    amplitudes: State<List<Int>>,
    endPositionMs: Float,
    progress: FloatState,
    maxWidthPx: Float,
    containerColor: Color,
    contentColor: Color,
    backgroundColor: Color,
    seekToProgress: (Float) -> Unit,
    height: Dp = 60.dp,
) {
    val width = remember { maxWidthPx * 2 }
    var isDragging by remember { mutableStateOf(false) }

    val currentProgress = remember { mutableFloatStateOf(progress.floatValue) }
    val currentPositionSeconds = remember { mutableLongStateOf((endPositionMs * progress.floatValue * 0.001).toLong()) }
    var position by remember { mutableIntStateOf((-width * progress.floatValue).roundToInt()) }

    LaunchedEffect(progress) {
        snapshotFlow { progress.floatValue }.collect { progress ->
            if (!isDragging) {
                currentPositionSeconds.longValue = (endPositionMs * progress * 0.001).toLong()
                currentProgress.floatValue = progress
                position = (-width * progress).roundToInt()
            }
        }
    }

    Box {
        Box(
            modifier = Modifier
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            seekToProgress(currentProgress.floatValue)
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val x = dragAmount.x.roundToInt()

                            change.consume()

                            if (position + x > 0) position = 0
                            else if (position + x < -width) position = -width.roundToInt()
                            else position += dragAmount.x.roundToInt()

                            currentProgress.floatValue = (position / width).absoluteValue
                            currentPositionSeconds.longValue =
                                ((endPositionMs / 1000) * currentProgress.floatValue).roundToLong()
                        },
                    )
                },
        ) {
            // The padding is to make the layout work when forcing a larger waveform height than the 48 dp the
            // Amplituda devs are for some reason trying to force:
            Box(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .padding(vertical = ((height - 48.dp) / 2).coerceAtLeast(0.dp))
            ) {
                WaveForm(
                    amplitudes = amplitudes,
                    currentProgress = currentProgress,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    maxWidthPx = maxWidthPx,
                    height = height,
                )
            }
        }

        TrackTimesRow(
            endPositionMs = endPositionMs,
            positionSeconds = currentPositionSeconds,
            backgroundColor = backgroundColor,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun TrackTimesRow(
    endPositionMs: Float,
    positionSeconds: LongState,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val textModifier = Modifier
        .background(backgroundColor)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
        .padding(3.dp)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = positionSeconds.longValue.seconds.sensibleFormat(),
            style = FistopyTheme.typography.labelSmall,
            modifier = textModifier,
            textAlign = TextAlign.Center,
        )
        Text(
            text = endPositionMs.toDouble().milliseconds.sensibleFormat(),
            style = FistopyTheme.typography.labelSmall,
            modifier = textModifier,
            textAlign = TextAlign.Center,
        )
    }
}
