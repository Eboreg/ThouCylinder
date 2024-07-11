package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BottomSheetItem(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(48.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        CompositionLocalProvider(
            value = LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            content = icon,
        )
        Spacer(Modifier.width(16.dp))
        CompositionLocalProvider(
            value = LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            content = text,
        )
    }
}

@Composable
fun BottomSheetItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    BottomSheetItem(
        icon = { Icon(imageVector = icon, contentDescription = null) },
        text = { Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        onClick = onClick,
    )
}

@Composable
fun BottomSheetItem(
    icon: Painter,
    text: String,
    onClick: () -> Unit,
) {
    BottomSheetItem(
        icon = { Icon(painter = icon, contentDescription = null) },
        text = { Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        onClick = onClick,
    )
}
