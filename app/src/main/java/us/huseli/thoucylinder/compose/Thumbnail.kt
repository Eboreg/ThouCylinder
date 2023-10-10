package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale

@Composable
fun Thumbnail(
    image: ImageBitmap?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    placeholder: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        Surface(shape = shape) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().aspectRatio(1f),
                )
            } else placeholder?.invoke()
        }
    }
}
