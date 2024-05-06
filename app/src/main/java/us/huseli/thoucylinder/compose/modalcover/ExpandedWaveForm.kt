package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.linc.audiowaveform.AudioWaveform
import com.linc.audiowaveform.model.AmplitudeType
import com.linc.audiowaveform.model.WaveformAlignment
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.Logger
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun ExpandedWaveForm(
    amplitudes: ImmutableList<Int>,
    endPositionMs: Float,
    progress: State<Float>,
    maxWidthPx: Float,
    containerColor: () -> Color,
    contentColor: () -> Color,
    backgroundColor: () -> Color,
    seekToProgress: (Float) -> Unit,
) {
    val density = LocalDensity.current
    var currentProgress by remember { mutableFloatStateOf(progress.value) }
    val currentPositionSeconds = remember { mutableLongStateOf(0) }
    val offsetX by remember { mutableIntStateOf(((maxWidthPx - with(density) { 40.dp.toPx() }) / 2).toInt()) }
    val width = remember { maxWidthPx * 2 }
    val widthDp = remember { with(density) { width.toDp() } }
    var position by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        snapshotFlow { progress.value }.collect {
            if (!isDragging) {
                currentPositionSeconds.longValue = (endPositionMs * it * 0.001).toLong()
                currentProgress = it
                position = (-width * it).roundToInt()
            }
        }
    }

    Box {
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            Logger.log("ModalCover", "onDragStart: offset.x=${offset.x}")
                            isDragging = true
                        },
                        onDragEnd = {
                            seekToProgress(currentProgress)
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            val x = dragAmount.x.roundToInt()

                            change.consume()

                            if (position + x > 0) position = 0
                            else if (position + x < -width) position = -width.roundToInt()
                            else position += dragAmount.x.roundToInt()

                            currentProgress = (position / width).absoluteValue
                            currentPositionSeconds.longValue = ((endPositionMs / 1000) * currentProgress).roundToLong()
                            Logger.log(
                                "ModalCover",
                                "onHorizontalDrag: dragAmount.x=${dragAmount.x}, position=$position, width=$width",
                            )
                        },
                    )
                },
        ) {
            Box(modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)) {
                AudioWaveform(
                    amplitudes = amplitudes,
                    onProgressChange = {},
                    style = Fill,
                    waveformAlignment = WaveformAlignment.Center,
                    amplitudeType = AmplitudeType.Avg,
                    progress = currentProgress,
                    waveformBrush = SolidColor(containerColor()),
                    progressBrush = SolidColor(contentColor()),
                    modifier = Modifier
                        .width(widthDp)
                        .absoluteOffset { IntOffset(x = offsetX + position, y = 0) }
                        .requiredHeight(60.dp)
                )
            }
        }

        TrackTimesRow(
            endPositionMs = endPositionMs,
            positionSeconds = currentPositionSeconds,
            backgroundColor = backgroundColor,
        )
    }
}

@Composable
fun BoxScope.TrackTimesRow(
    endPositionMs: Float,
    positionSeconds: LongState,
    backgroundColor: () -> Color,
) {
    val textModifier = Modifier
        .background(backgroundColor())
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
        .padding(3.dp)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
    ) {
        Text(
            text = positionSeconds.longValue.seconds.sensibleFormat(),
            style = MaterialTheme.typography.labelSmall,
            modifier = textModifier,
            textAlign = TextAlign.Center,
        )
        Text(
            text = endPositionMs.toDouble().milliseconds.sensibleFormat(),
            style = MaterialTheme.typography.labelSmall,
            modifier = textModifier,
            textAlign = TextAlign.Center,
        )
    }
}
