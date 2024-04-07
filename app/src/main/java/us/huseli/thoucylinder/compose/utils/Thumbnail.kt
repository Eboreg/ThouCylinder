package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Thumbnail(
    imageBitmap: () -> ImageBitmap?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    borderWidth: Dp? = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    placeholderPadding: PaddingValues = PaddingValues(10.dp),
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
) {
    Surface(
        shape = shape,
        modifier = modifier.aspectRatio(1f).fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = borderWidth?.let { BorderStroke(it, borderColor) },
    ) {
        val image = imageBitmap()

        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        } else if (placeholderIcon != null) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                modifier = Modifier.padding(placeholderPadding).fillMaxSize(),
                tint = placeholderIconTint ?: LocalContentColor.current,
            )
        }
    }
}
