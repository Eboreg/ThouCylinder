package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ItemGrid(
    things: List<T>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    onClick: ((T) -> Unit)? = null,
    onLongClick: ((T) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    isSelected: (T) -> Boolean = { false },
    onEmpty: (@Composable () -> Unit)? = null,
    cardContent: @Composable ColumnScope.(T) -> Unit,
) {
    if (things.isEmpty() && onEmpty != null) onEmpty()

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 10.dp),
        columns = GridCells.Adaptive(minSize = 160.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding,
    ) {
        items(things, key = key) { thing ->
            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.let {
                    if (onClick != null || onLongClick != null) it.combinedClickable(
                        onClick = { onClick?.invoke(thing) },
                        onLongClick = { onLongClick?.invoke(thing) },
                    ) else it
                },
                content = { cardContent(thing) },
                border = CardDefaults.outlinedCardBorder().let {
                    if (isSelected(thing)) it.copy(width = it.width + 2.dp) else it
                },
            )
        }
    }
}
