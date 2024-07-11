package us.huseli.thoucylinder.compose.scrollbar

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.ceil

@Composable
fun rememberScrollbarGridState(
    gridState: LazyGridState = rememberLazyGridState(),
    key: String? = null,
    contentType: String? = null,
): ScrollbarGridState = rememberSaveable(saver = ScrollbarGridState.Saver, key = key) {
    ScrollbarGridState(gridState = gridState, contentType = contentType)
}

@Stable
class ScrollbarGridState(
    val gridState: LazyGridState,
    progress: Float = 0f,
    contentType: String? = null,
) : AbstractScrollbarState(progress = progress, contentType = contentType) {
    private val itemsPerRowAndRowHeight: Pair<Int, Float> by derivedStateOf {
        var result = Pair(1, 0f)

        gridState.layoutInfo.visibleItemsInfo
            .filter { contentType == null || it.contentType == contentType }
            .takeIf { it.size > 1 }
            ?.let { items ->
                val firstOffsetY = items.first().offset.y

                for ((index, item) in items.withIndex()) {
                    if (item.offset.y > firstOffsetY) {
                        result = Pair(index, (item.offset.y - firstOffsetY).toFloat())
                        break
                    }
                }
            }
        result
    }

    override val afterContentPadding: Int
        get() = gridState.layoutInfo.afterContentPadding

    override val beforeContentPadding: Int
        get() = gridState.layoutInfo.beforeContentPadding

    override val debugLogValues: Map<String, Any?> by derivedStateOf {
        super.debugLogValues + mapOf(
            "firstVisibleItemIndex" to gridState.firstVisibleItemIndex,
            "firstVisibleItemScrollOffset" to gridState.firstVisibleItemScrollOffset,
            "totalItemsCount" to gridState.layoutInfo.totalItemsCount,
        )
    }

    override val firstVisibleRowIndex: Int by derivedStateOf {
        val firstVisibleItemIndex = contentType
            ?.let { gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.contentType == contentType }?.index }
            ?: gridState.firstVisibleItemIndex

        ceil(firstVisibleItemIndex.toFloat() / itemsPerRow).toInt()
    }

    override val firstVisibleRowScrollOffset: Int by derivedStateOf {
        contentType
            ?.let { gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.contentType == contentType }?.offset?.y }
            ?: gridState.firstVisibleItemScrollOffset
    }

    override val itemsPerRow: Int by derivedStateOf {
        itemsPerRowAndRowHeight.first
    }

    override val mainAxisItemSpacing: Int
        get() = gridState.layoutInfo.mainAxisItemSpacing

    override val rowHeight: Float by derivedStateOf {
        itemsPerRowAndRowHeight.second
    }

    /*
    override val listHeight: Float by derivedStateOf {
        (rowHeight * totalRowCount) + gridState.layoutInfo.beforeContentPadding + gridState.layoutInfo.afterContentPadding
    }
     */

    override val totalRowCount: Int by derivedStateOf {
        val totalItemsCount = contentType
            ?.let { gridState.layoutInfo.visibleItemsInfo.filter { it.contentType == contentType }.size }
            ?: gridState.layoutInfo.totalItemsCount

        ceil(totalItemsCount.toFloat() / itemsPerRow).toInt()
    }

    /*
    override fun getAbsoluteScrollTop(): Float {
        val rowsBeforeFirst = gridState.firstVisibleItemIndex / itemsPerRow

        return gridState.firstVisibleItemScrollOffset +
            (if (rowsBeforeFirst > 0) gridState.layoutInfo.beforeContentPadding else 0) +
            ((rowsBeforeFirst - 1).coerceAtLeast(0) * gridState.layoutInfo.mainAxisItemSpacing) +
            (rowsBeforeFirst * rowHeight)
    }
     */

    override fun itemIsVisible(key: Any?): Boolean = gridState.layoutInfo.visibleItemsInfo.any { it.key == key }

    override suspend fun scrollBy(pixels: Float) {
        gridState.scroll { scrollBy(pixels) }
    }

    override suspend fun scrollToItem(index: Int, scrollOffset: Int) {
        gridState.scrollToItem(index, scrollOffset)
    }

    override fun toString(): String = "ScrollbarGridState[$debugLogString]"

    companion object {
        val Saver: Saver<ScrollbarGridState, *> = listSaver(
            save = {
                listOf(
                    it.gridState.firstVisibleItemIndex.toString(),
                    it.gridState.firstVisibleItemScrollOffset.toString(),
                    it.progress.toString(),
                    it.contentType,
                )
            },
            restore = {
                ScrollbarGridState(
                    gridState = LazyGridState(
                        firstVisibleItemIndex = it[0]!!.toInt(),
                        firstVisibleItemScrollOffset = it[1]!!.toInt(),
                    ),
                    progress = it[2]!!.toFloat(),
                    contentType = it[3],
                )
            },
        )
    }
}
