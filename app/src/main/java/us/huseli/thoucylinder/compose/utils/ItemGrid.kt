package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ItemGrid(
    things: List<T>,
    modifier: Modifier = Modifier,
    onCardClick: ((T) -> Unit)? = null,
    cardContent: @Composable ColumnScope.(T) -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 160.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(things) { thing ->
            val cardModifier =
                if (onCardClick != null) Modifier.clickable { onCardClick(thing) }
                else Modifier

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = cardModifier,
                content = { cardContent(thing) },
            )
        }
    }
}
