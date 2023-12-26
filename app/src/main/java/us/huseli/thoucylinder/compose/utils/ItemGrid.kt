package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
    key: (index: Int, item: T) -> Any,
    onClick: (Int, T) -> Unit,
    onLongClick: ((Int, T) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    isSelected: (T) -> Boolean = { false },
    onEmpty: @Composable () -> Unit,
    cardContent: @Composable ColumnScope.(Int, T) -> Unit,
) {
    if (things.isEmpty()) onEmpty()

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 10.dp),
        columns = GridCells.Adaptive(minSize = 160.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(things, key = key) { index, thing ->
            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(index, thing) },
                    onLongClick = { onLongClick?.invoke(index, thing) },
                ),
                content = { cardContent(index, thing) },
                border = CardDefaults.outlinedCardBorder().let {
                    if (isSelected(thing)) it.copy(width = it.width + 2.dp) else it
                },
            )
        }
    }
}
