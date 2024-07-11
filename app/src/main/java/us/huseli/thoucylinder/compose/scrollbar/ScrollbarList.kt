package us.huseli.thoucylinder.compose.scrollbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScrollbarList(
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
    state: ScrollbarListState = rememberScrollbarListState(),
    barColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    handleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    handleHeight: Dp = 40.dp,
    handleWidth: Dp = 15.dp,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    contentType: String? = null,
    content: LazyListScope.() -> Unit,
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
        LazyColumn(
            modifier = modifier,
            state = state.listState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
