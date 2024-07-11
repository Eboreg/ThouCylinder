package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ThumbnailImage(
    model: Any?,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
    customModelSize: Int? = null,
) {
    val context = LocalContext.current
    var loadFailed by remember(model) { mutableStateOf(false) }
    val request = with(ImageRequest.Builder(context).data(model)) {
        if (customModelSize != null) size(customModelSize)
        build()
    }

    if (!loadFailed) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = { loadFailed = true },
            modifier = modifier,
        )
    } else if (placeholderIcon != null) {
        Icon(
            imageVector = placeholderIcon,
            contentDescription = null,
            modifier = modifier.padding(10.dp).fillMaxSize(),
            tint = placeholderIconTint ?: LocalContentColor.current,
        )
    }
}

@Composable
fun Thumbnail(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    borderWidth: Dp? = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
    customModelSize: Int? = null,
) {
    Surface(
        shape = shape,
        modifier = modifier.aspectRatio(1f).fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = borderWidth?.let { BorderStroke(it, borderColor) },
        content = {
            ThumbnailImage(
                model = model,
                placeholderIcon = placeholderIcon,
                placeholderIconTint = placeholderIconTint,
                customModelSize = customModelSize,
            )
        },
    )
}

@Composable
fun Thumbnail4x4(
    models: List<Any?>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    borderWidth: Dp? = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
    customModelSize: Int? = null,
) {
    if (models.size < 4) {
        Thumbnail(
            model = models.firstOrNull(),
            modifier = modifier,
            shape = shape,
            borderWidth = borderWidth,
            borderColor = borderColor,
            placeholderIcon = placeholderIcon,
            placeholderIconTint = placeholderIconTint,
            customModelSize = customModelSize,
        )
    } else {
        Surface(
            shape = shape,
            modifier = modifier.aspectRatio(1f).fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = borderWidth?.let { BorderStroke(it, borderColor) },
        ) {
            Column {
                for (chunk in models.subList(0, 4).chunked(2)) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (model in chunk) {
                            ThumbnailImage(
                                model = model,
                                placeholderIcon = placeholderIcon,
                                placeholderIconTint = placeholderIconTint,
                                modifier = Modifier.weight(1f),
                                customModelSize = customModelSize?.div(4),
                            )
                        }
                    }
                }
            }
        }
    }
}
