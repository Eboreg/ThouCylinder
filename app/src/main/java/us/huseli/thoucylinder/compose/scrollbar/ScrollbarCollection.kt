package us.huseli.thoucylinder.compose.scrollbar

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun ScrollbarCollection(
    state: AbstractScrollbarState,
    barColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    handleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    minHandleHeight: Dp = 40.dp,
    @SuppressLint("ModifierParameter")
    handleModifier: Modifier = Modifier,
    handleWidth: Dp = 15.dp,
    contentType: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (contentType != null) state.contentType = contentType

    BoxWithConstraints {
        val density = LocalDensity.current

        LaunchedEffect(maxHeight) {
            state.viewportHeight = with(density) { maxHeight.toPx() }
        }

        LaunchedEffect(minHandleHeight) {
            state.setMinHandleHeight(with(density) { minHandleHeight.toPx() })
        }

        LaunchedEffect(state) {
            state.listScrollDeltas.receiveAsFlow().collect { delta ->
                state.scrollBy(delta)
            }
        }

        LaunchedEffect(state) {
            snapshotFlow { state.firstVisibleRowScrollOffset }.distinctUntilChanged().collect {
                state.redrawIfNotDragging()
            }
        }

        LaunchedEffect(state) {
            snapshotFlow { state.totalRowCount }.distinctUntilChanged().collect {
                state.redrawIfNotDragging()
            }
        }

        Row {
            Column(modifier = Modifier.weight(1f), content = content)

            if (state.shouldDisplayScrollbar) {
                Box(
                    modifier = Modifier
                        .width(handleWidth)
                        .fillMaxHeight()
                        .background(barColor)
                ) {
                    Surface(
                        color = handleColor,
                        modifier = handleModifier
                            .size(width = handleWidth, height = with(density) { state.handleHeight.toDp() })
                            .scrollbar(state = state)
                    ) {}
                }
            }
        }
    }
}
