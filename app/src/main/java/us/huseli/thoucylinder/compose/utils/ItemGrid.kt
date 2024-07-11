package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarGrid
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarGridState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ItemGrid(
    things: () -> ImmutableList<T>,
    key: (T) -> Any,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    scrollbarState: ScrollbarGridState = rememberScrollbarGridState(),
    isLoading: Boolean = false,
    contentType: String? = null,
    onLongClick: ((T) -> Unit)? = null,
    isSelected: (T) -> Boolean = { false },
    trailingContent: @Composable (() -> Unit)? = null,
    onEmpty: @Composable () -> Unit = {},
    cardContent: @Composable (ColumnScope.(T) -> Unit),
) {
    if (isLoading) IsLoadingProgressIndicator()

    ScrollbarGrid(
        state = scrollbarState,
        modifier = modifier,
        contentPadding = PaddingValues(10.dp),
        contentType = contentType,
    ) {
        if (things().isEmpty() && !isLoading) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = { onEmpty() })
        }

        items(things(), key = key, contentType = { contentType }) { thing ->
            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(thing) },
                    onLongClick = { onLongClick?.invoke(thing) },
                ),
                content = { cardContent(thing) },
                border = CardDefaults.outlinedCardBorder().let {
                    if (isSelected(thing)) it.copy(width = it.width + 2.dp) else it
                },
            )
        }
        trailingContent?.also { item(span = { GridItemSpan(maxLineSpan) }) { it() } }
    }
}
