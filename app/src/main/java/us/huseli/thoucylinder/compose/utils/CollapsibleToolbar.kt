package us.huseli.thoucylinder.compose.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CollapsibleToolbar(
    modifier: Modifier = Modifier,
    show: Boolean = true,
    tonalElevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.animateContentSize().heightIn(max = if (show) Dp.Infinity else 0.dp),
        color = BottomAppBarDefaults.containerColor,
        tonalElevation = tonalElevation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 10.dp, top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
inline fun rememberToolbarScrollConnection(
    limit: Int = 10,
    crossinline onShowToolbarChange: @DisallowComposableCalls (Boolean) -> Unit,
) = remember {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -limit) onShowToolbarChange(false)
            else if (available.y > limit) onShowToolbarChange(true)
            return Offset.Zero
        }
    }
}
