package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun ListWithNumericBar2(
    modifier: Modifier = Modifier,
    barModifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
    listState: LazyListState = rememberLazyListState(),
    minItems: Int = 30,
    barWidth: Dp = 30.dp,
    listSize: Int,
    displayOffset: Int = 1,
    itemHeight: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var maxHeightDp by remember { mutableStateOf(0.dp) }
    var itemIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var itemInterval by remember { mutableIntStateOf(0) }
    val itemsPerScreen by remember(maxHeightDp) {
        mutableStateOf(itemHeight?.let { itemHeight -> (maxHeightDp / itemHeight).toInt().takeIf { it > 0 } })
    }
    val showNumericBar by remember(itemIndices, listSize, minItems) {
        mutableStateOf(itemIndices.isNotEmpty() && listSize >= minItems)
    }

    LaunchedEffect(maxHeightDp, listSize) {
        val maxIndexCount = (maxHeightDp / 30.dp).toInt()
        val indexCount = itemsPerScreen?.let { kotlin.math.min(listSize / it, maxIndexCount) } ?: maxIndexCount

        if (indexCount > 1 && listSize >= minItems) {
            val increment = listSize / indexCount
            val tempIndices = mutableSetOf<Int>()

            for (i in 0 until listSize step increment) {
                tempIndices.add(i)
            }
            itemInterval = increment
            itemIndices = tempIndices.toList()
        }
    }

    BoxWithConstraints(modifier = modifier) {
        maxHeightDp = maxHeight
        Row {
            Column(modifier = Modifier.weight(1f)) {
                content()
            }

            if (showNumericBar) {
                var selected by remember { mutableIntStateOf(0) }

                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }.collect { firstVisibleItemIndex ->
                        val itemIndex =
                            itemsPerScreen?.let { firstVisibleItemIndex + (it / 2) } ?: firstVisibleItemIndex
                        selected = itemIndices.minBy { (itemIndex - it).absoluteValue }
                    }
                }

                Column(
                    modifier = barModifier.width(barWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    itemIndices.forEach { itemIndex ->
                        Surface(
                            shape = CircleShape,
                            color = if (itemIndex == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (itemIndex == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .padding(vertical = 1.dp)
                                .padding(start = 2.dp)
                                .size(min(barWidth, 30.dp) - 2.dp)
                                .clickable(
                                    onClick = {
                                        scope.launch {
                                            listState.scrollToItem(itemIndex)
                                            selected = itemIndex
                                        }
                                    },
                                    indication = rememberRipple(bounded = false, radius = (barWidth / 2) + 5.dp),
                                    interactionSource = remember { MutableInteractionSource() },
                                ),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = (itemIndex + displayOffset).toString(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
