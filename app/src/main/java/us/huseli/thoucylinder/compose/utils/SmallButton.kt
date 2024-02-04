package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SmallButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    text: String,
    height: Dp = 32.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        modifier = modifier.height(height),
        onClick = onClick,
        shape = shape,
        contentPadding = contentPadding,
        enabled = enabled,
        colors = colors,
        content = {
            if (leadingIcon != null) Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(height - 4.dp),
            )
            Text(text = text, style = textStyle, modifier = Modifier.padding(horizontal = 5.dp))
        },
    )
}
