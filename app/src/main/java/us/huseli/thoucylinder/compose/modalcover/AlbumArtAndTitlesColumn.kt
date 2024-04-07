package us.huseli.thoucylinder.compose.modalcover

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Suppress("AnimateAsStateLabel")
@Composable
fun AlbumArtAndTitlesColumn(
    imageBitmap: () -> ImageBitmap?,
    title: String,
    artist: String?,
    offsetX: Int,
    contentAlpha: Float,
    showTitles: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val animationSpec = tween<Dp>(150)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val width by animateDpAsState(
        if (isExpanded && !isLandscape) configuration.screenWidthDp.dp
        else if (isExpanded) 250.dp
        else 66.dp,
        animationSpec,
    )

    Column(
        modifier = modifier.width(width).offset { IntOffset(x = offsetX, y = 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AlbumArtColumn(imageBitmap = imageBitmap)
        if (showTitles) TitlesColumn(isExpanded = true, title = title, artist = artist, alpha = contentAlpha)
    }
}