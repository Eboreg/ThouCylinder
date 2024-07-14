package us.huseli.thoucylinder.compose.scrollbar

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.channels.Channel
import us.huseli.thoucylinder.interfaces.ILogger
import kotlin.math.pow
import kotlin.math.roundToInt

abstract class AbstractScrollbarState(progress: Float = 0f, contentType: String? = null) : ILogger {
    /** The heights below are only used to calculate listToBarRatio. */
    private var _minHandleHeight: Float by mutableFloatStateOf(0f)
    private var hasScrolledOnLoad = false

    private val absoluteScrollTop: Float by derivedStateOf {
        firstVisibleRowScrollOffset + (firstVisibleRowIndex * rowHeight)
    }
    private val listHeight: Float by derivedStateOf { rowHeight * totalRowCount }
    private val maxHandleOffsetY: Float by derivedStateOf { (viewportHeight - handleHeight).coerceAtLeast(0f) }
    private val maxListScrollTop: Float by derivedStateOf { (listHeight - viewportHeight).coerceAtLeast(0f) }

    protected open val debugLogValues: Map<String, Any?> by derivedStateOf {
        mapOf(
            "absoluteScrollTop" to absoluteScrollTop,
            "afterContentPadding" to afterContentPadding,
            "beforeContentPadding" to beforeContentPadding,
            "firstVisibleRowIndex" to firstVisibleRowIndex,
            "firstVisibleRowScrollOffset" to firstVisibleRowScrollOffset,
            "handleHeight" to handleHeight,
            "handleOffsetY" to handleOffsetY,
            "isDraggingHandle" to isDraggingHandle,
            "itemsPerRow" to itemsPerRow,
            "listHeight" to listHeight,
            "mainAxisItemSpacing" to mainAxisItemSpacing,
            "maxHandleOffsetY" to maxHandleOffsetY,
            "maxListScrollTop" to maxListScrollTop,
            "progress" to this.progress,
            "rowHeight" to rowHeight,
            "totalRowCount" to totalRowCount,
            "viewportHeight" to viewportHeight,
        )
    }

    protected val debugLogString by derivedStateOf {
        debugLogValues.toSortedMap().toList().joinToString(", ") { "${it.first}=${it.second}" }
    }

    var contentType: String? by mutableStateOf(contentType)
    var handleOffsetY: Float by mutableFloatStateOf(0f)
        private set
    var isDraggingHandle: Boolean by mutableStateOf(false)
    var progress: Float by mutableFloatStateOf(progress)
        private set
    var viewportHeight: Float by mutableFloatStateOf(0f)

    val handleHeight: Float by derivedStateOf {
        (viewportHeight.pow(2) / listHeight).coerceIn(
            _minHandleHeight,
            viewportHeight.coerceAtLeast(_minHandleHeight),
        )
    }
    val listScrollDeltas = Channel<Float>()
    val shouldDisplayScrollbar: Boolean by derivedStateOf { listHeight > viewportHeight * 5f }

    abstract val afterContentPadding: Int
    abstract val beforeContentPadding: Int
    abstract val firstVisibleRowScrollOffset: Int
    abstract val firstVisibleRowIndex: Int
    abstract val itemsPerRow: Int
    abstract val mainAxisItemSpacing: Int
    abstract val rowHeight: Float
    abstract val totalRowCount: Int

    abstract fun itemIsVisible(key: Any?): Boolean
    abstract suspend fun scrollBy(pixels: Float)
    abstract suspend fun scrollToItem(index: Int, scrollOffset: Int = 0)

    fun onHandleDrag(delta: Float) {
        /** Send new list delta when the handle has been dragged. Also update progress and handle offset. */
        handleOffsetY = (handleOffsetY + delta).coerceIn(0f, maxHandleOffsetY)
        progress = if (maxHandleOffsetY > 0f) (handleOffsetY / maxHandleOffsetY).coerceIn(0f, 1f) else 0f
        listScrollDeltas.trySend(delta * getListToBarRatio())
        logState("onHandleDrag($delta)")
    }

    fun redrawIfNotDragging() {
        /**
         * Called when the list has been scrolled for whatever reason. If it's _not_ due to the handle being dragged,
         * set new handle offset value.
         */
        if (!isDraggingHandle) {
            progress = if (maxListScrollTop > 0f) (absoluteScrollTop / maxListScrollTop).coerceIn(0f, 1f) else 0f
            handleOffsetY = maxHandleOffsetY * progress
        }
        logState("redrawIfNotDragging: absoluteScrollTop=$absoluteScrollTop")
    }

    suspend fun scrollToIndexOnLoad(index: Int, scrollOffset: Int = 0, key: Any? = null) {
        if (!hasScrolledOnLoad) {
            snapshotFlow { totalRowCount }.collect { rowCount ->
                if ((rowCount * itemsPerRow) > index) {
                    hasScrolledOnLoad = true
                    if (key == null || !itemIsVisible(key)) {
                        scrollToItem(index, scrollOffset)
                    }
                }
            }
        }
    }

    fun setMinHandleHeight(value: Float) {
        _minHandleHeight = value
    }

    private fun getListToBarRatio(): Float = calculateListToBarRatio(viewportHeight, handleHeight, listHeight)

    private fun logState(prefix: String) {
        if (LOGGING_ENABLED) log("[$prefix] $this")
    }

    companion object {
        const val LOGGING_ENABLED = false

        fun calculateListToBarRatio(viewportHeight: Float, handleHeight: Float, listHeight: Float): Float {
            val maxHandleOffset = viewportHeight - handleHeight

            return if (maxHandleOffset == 0f) 0f
            else (listHeight - viewportHeight) / maxHandleOffset
        }
    }
}

@Stable
fun Modifier.scrollbar(state: AbstractScrollbarState) =
    offset { IntOffset(x = 0, y = state.handleOffsetY.roundToInt()) }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { state.isDraggingHandle = true },
                onDragCancel = { state.isDraggingHandle = false },
                onDragEnd = { state.isDraggingHandle = false },
                onDrag = { change, dragAmount ->
                    state.onHandleDrag(dragAmount.y)
                    change.consume()
                },
            )
        }
