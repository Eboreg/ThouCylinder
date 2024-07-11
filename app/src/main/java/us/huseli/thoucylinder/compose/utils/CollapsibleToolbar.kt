package us.huseli.thoucylinder.compose.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.Logger

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 2.dp,
    verticalSpacing: Dp = 5.dp,
    padding: PaddingValues = PaddingValues(bottom = 5.dp, start = 10.dp, end = 10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = tonalElevation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

@Composable
fun CollapsibleToolbar(
    show: () -> Boolean,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 2.dp,
    verticalSpacing: Dp = 5.dp,
    padding: PaddingValues = PaddingValues(bottom = 5.dp, start = 10.dp, end = 10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = show(),
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Toolbar(
            modifier = modifier,
            tonalElevation = tonalElevation,
            content = content,
            verticalSpacing = verticalSpacing,
            padding = padding,
        )
    }
}

@Composable
inline fun rememberToolbarScrollConnection(
    limit: Int = 10,
    crossinline onShowToolbarChange: @DisallowComposableCalls (Boolean) -> Unit,
) = remember(limit) {
    object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            Logger.log("rememberToolbarScrollConnection", "consumed=$consumed, available=$available")
            if (consumed.y < -limit) onShowToolbarChange(false)
            else if (available.y > limit || consumed.y > limit) onShowToolbarChange(true)
            return Offset.Zero
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
    }
}
