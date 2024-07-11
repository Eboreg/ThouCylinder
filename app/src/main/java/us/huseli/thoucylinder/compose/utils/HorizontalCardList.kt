package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> HorizontalCardList(
    things: () -> List<T>,
    key: (T) -> Any,
    thumbnailModel: (T) -> Any?,
    modifier: Modifier = Modifier,
    cardHeight: Dp = 150.dp,
    cardWidth: Dp = 100.dp,
    horizontalOffset: Dp = (-10).dp,
    onClick: ((T) -> Unit)? = null,
    text: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .layout { measurable, constraints ->
                val offset = with(density) { horizontalOffset.roundToPx() }
                val width = constraints.maxWidth - (offset * 2)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width)
                )

                layout(width = width, height = placeable.height) {
                    placeable.place(0, 0)
                }
            }
    ) {
        itemsIndexed(things(), key = { _, item -> key(item) }) { index, thing ->
            Card(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .padding(
                        start = if (index == 0) -horizontalOffset else 0.dp,
                        end = if (index == things().size - 1) -horizontalOffset else 0.dp,
                    )
                    .height(cardHeight)
                    .width(cardWidth)
                    .then(onClick?.let { Modifier.clickable { it(thing) } } ?: Modifier)
            ) {
                Thumbnail(model = thumbnailModel(thing), shape = RectangleShape, borderWidth = null)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    content = { text(thing) },
                )
            }
        }
    }
}
