package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
    cardHeight: Dp? = 80.dp,
    isSelected: (T) -> Boolean = { false },
    onClick: ((T) -> Unit)? = null,
    onLongClick: ((T) -> Unit)? = null,
    cardModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    cardContent: @Composable ColumnScope.(T) -> Unit,
) {
    ListWithNumericBar(listState = listState, listSize = things.size, minItems = 20, modifier = modifier) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = contentPadding,
        ) {
            items(things) { thing ->
                val containerColor =
                    if (isSelected(thing)) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface

                Card(
                    colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = cardModifier.fillMaxWidth()
                        .let { if (cardHeight != null) it.height(cardHeight) else it }
                        .let {
                            if (onClick != null || onLongClick != null) it.combinedClickable(
                                onClick = { onClick?.invoke(thing) },
                                onLongClick = onLongClick?.let { { it(thing) } },
                            ) else it
                        },
                    content = { cardContent(thing) },
                )
            }
        }
    }
}
