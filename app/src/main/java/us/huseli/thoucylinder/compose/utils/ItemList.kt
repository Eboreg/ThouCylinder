package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarList
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarListState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState

@Composable
fun <T> ItemList(
    things: () -> ImmutableList<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    contentType: String? = null,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(),
    isLoading: Boolean = false,
    onEmpty: @Composable () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    if (isLoading) IsLoadingProgressIndicator()

    ScrollbarList(
        state = scrollbarState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(10.dp),
        contentType = contentType,
    ) {
        if (things().isEmpty() && !isLoading) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = { onEmpty() })
        }
        items(things(), key = key, itemContent = itemContent, contentType = { contentType })
        trailingContent?.also { item { it() } }
    }
}

@Composable
fun <T> ItemListReorderable(
    things: () -> ImmutableList<T>,
    key: (T) -> Any,
    reorderableState: ReorderableLazyListState,
    modifier: Modifier = Modifier,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(listState = reorderableState.listState),
    contentType: String? = null,
    isLoading: Boolean = false,
    onEmpty: @Composable () -> Unit = {},
    itemContent: @Composable LazyItemScope.(T, Boolean) -> Unit,
) {
    ItemList(
        things = things,
        key = key,
        scrollbarState = scrollbarState,
        isLoading = isLoading,
        onEmpty = onEmpty,
        modifier = modifier.reorderable(reorderableState),
        contentType = contentType,
    ) { thing ->
        ReorderableItem(state = reorderableState, key = key(thing)) { isDragging ->
            itemContent(thing, isDragging)
        }
    }
}
