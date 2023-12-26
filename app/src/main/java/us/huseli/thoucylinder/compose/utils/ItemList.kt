package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.compose.ListWithNumericBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ItemList(
    things: List<T>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    cardHeight: Dp? = 70.dp,
    gap: Dp = 8.dp,
    key: ((index: Int, item: T) -> Any)? = null,
    border: BorderStroke? = null,
    isSelected: (T) -> Boolean = { false },
    onClick: ((Int, T) -> Unit)? = null,
    onLongClick: ((Int, T) -> Unit)? = null,
    cardModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    padding: PaddingValues = PaddingValues(horizontal = 10.dp),
    showNumericBarAtItemCount: Int = 20,
    onEmpty: (@Composable () -> Unit)? = null,
    leadingItem: (@Composable LazyItemScope.() -> Unit)? = null,
    trailingItem: (@Composable LazyItemScope.() -> Unit)? = null,
    stickyHeaderContent: (@Composable LazyItemScope.() -> Unit)? = null,
    cardContent: @Composable ColumnScope.(Int, T) -> Unit,
) {
    if (things.isEmpty() && onEmpty != null) onEmpty()

    ListWithNumericBar(
        listState = listState,
        listSize = things.size,
        minItems = showNumericBarAtItemCount,
        modifier = modifier.padding(padding),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(gap),
            contentPadding = contentPadding,
        ) {
            leadingItem?.also { item { it() } }
            stickyHeaderContent?.also { stickyHeader { it() } }

            itemsIndexed(things, key = key) { index, thing ->
                val containerColor =
                    if (isSelected(thing)) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface

                Card(
                    border = border,
                    colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = cardModifier.fillMaxWidth()
                        .let { if (cardHeight != null) it.height(cardHeight) else it }
                        .let {
                            if (onClick != null || onLongClick != null) it.combinedClickable(
                                onClick = { onClick?.invoke(index, thing) },
                                onLongClick = onLongClick?.let { { it(index, thing) } },
                            ) else it
                        },
                    content = { cardContent(index, thing) },
                )
            }

            trailingItem?.also { item { it() } }
        }
    }
}
