package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SmallOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    enabled: Boolean = true,
    height: Dp = 32.dp,
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        enabled = enabled,
        border = border,
        content = content,
    )
}

@Composable
fun SmallOutlinedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    enabled: Boolean = true,
    height: Dp = 32.dp,
    leadingIcon: ImageVector? = null,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    SmallOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = border,
        colors = colors,
        contentPadding = contentPadding,
        enabled = enabled,
        height = height,
        shape = shape,
        content = {
            if (leadingIcon != null) Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(height).padding(end = 5.dp),
            )
            Text(text = text, style = textStyle)
        },
    )
}
