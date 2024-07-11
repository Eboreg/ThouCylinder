package us.huseli.thoucylinder.compose.scrollbar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun rememberScrollbarListState(
    listState: LazyListState = rememberLazyListState(),
    key: String? = null,
    contentType: String? = null,
): ScrollbarListState = rememberSaveable(saver = ScrollbarListState.Saver, key = key) {
    ScrollbarListState(listState = listState, contentType = contentType)
}

@Stable
class ScrollbarListState(
    val listState: LazyListState,
    progress: Float = 0f,
    contentType: String? = null,
) : AbstractScrollbarState(progress = progress, contentType = contentType) {
    override val afterContentPadding: Int
        get() = listState.layoutInfo.afterContentPadding

    override val beforeContentPadding: Int
        get() = listState.layoutInfo.beforeContentPadding

    override val firstVisibleRowIndex: Int by derivedStateOf {
        contentType
            ?.let { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.contentType == contentType }?.index }
            ?: listState.firstVisibleItemIndex
    }

    override val firstVisibleRowScrollOffset: Int by derivedStateOf {
        contentType
            ?.let { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.contentType == contentType }?.offset }
            ?: listState.firstVisibleItemScrollOffset
    }

    override val itemsPerRow: Int = 1

    override val mainAxisItemSpacing: Int
        get() = listState.layoutInfo.mainAxisItemSpacing

    override val rowHeight: Float by derivedStateOf {
        listState.layoutInfo.visibleItemsInfo.takeIf { it.size > 1 }?.let { items ->
            val first = items.first()
            val last = items.last()

            (last.offset - first.offset).toFloat() / (last.index - first.index)
        } ?: 0f
    }

    override val totalRowCount: Int
        get() = listState.layoutInfo.totalItemsCount

    override fun itemIsVisible(key: Any?): Boolean = listState.layoutInfo.visibleItemsInfo.any { it.key == key }

    override suspend fun scrollBy(pixels: Float) {
        listState.scroll { scrollBy(pixels) }
    }

    override suspend fun scrollToItem(index: Int, scrollOffset: Int) {
        listState.scrollToItem(index, scrollOffset)
    }

    override fun toString(): String = "ScrollbarListState[$debugLogString]"

    companion object {
        val Saver: Saver<ScrollbarListState, *> = listSaver(
            save = {
                listOf(
                    it.listState.firstVisibleItemIndex.toString(),
                    it.listState.firstVisibleItemScrollOffset.toString(),
                    it.progress.toString(),
                    it.contentType,
                )
            },
            restore = {
                ScrollbarListState(
                    listState = LazyListState(
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
