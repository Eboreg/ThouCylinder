package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Thumbnail(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    borderWidth: Dp = 1.dp,
    placeholder: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = shape,
        modifier = modifier.aspectRatio(1f).fillMaxSize(),
        color = Color.Transparent,
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        } else placeholder?.invoke()
    }
}
