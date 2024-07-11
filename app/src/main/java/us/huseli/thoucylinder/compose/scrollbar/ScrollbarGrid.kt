package us.huseli.thoucylinder.compose.scrollbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScrollbarGrid(
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    columns: GridCells = GridCells.Adaptive(minSize = 160.dp),
    state: ScrollbarGridState = rememberScrollbarGridState(),
    handleHeight: Dp = 40.dp,
    handleWidth: Dp = 15.dp,
    barColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    handleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp),
    contentType: String? = null,
    content: LazyGridScope.() -> Unit,
) {
    ScrollbarCollection(
        barColor = barColor,
        handleColor = handleColor,
        minHandleHeight = handleHeight,
        handleModifier = handleModifier,
        handleWidth = handleWidth,
        state = state,
        contentType = contentType,
    ) {
        LazyVerticalGrid(
            columns = columns,
            modifier = modifier,
            state = state.gridState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
    }
}
