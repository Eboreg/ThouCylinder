package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.compose.ListWithAlphabetBar
import us.huseli.thoucylinder.leadingChars

@Composable
fun <T> ItemList(
    things: List<T>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onCardClick: ((T) -> Unit)? = null,
    selector: (T) -> String,
    cardContent: @Composable ColumnScope.(T) -> Unit,
) {
    ListWithAlphabetBar(
        modifier = modifier,
        characters = things.map(selector).leadingChars(),
        listState = listState,
        items = things,
        selector = selector,
        minItems = 20,
    ) {
        val paddingValues =
            if (things.size >= 20) PaddingValues(start = 10.dp)
            else PaddingValues(horizontal = 10.dp)

        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(things) { thing ->
                val cardModifier =
                    if (onCardClick != null) Modifier.clickable { onCardClick(thing) }
                    else Modifier

                OutlinedCard(
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = cardModifier.fillMaxWidth().height(80.dp),
                    content = { cardContent(thing) },
                )
            }
        }
    }
}
