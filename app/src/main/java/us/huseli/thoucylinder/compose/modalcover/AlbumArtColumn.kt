package us.huseli.thoucylinder.compose.modalcover

import android.content.res.Configuration
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.compose.utils.Thumbnail

@Composable
fun AlbumArtColumn(imageBitmap: () -> ImageBitmap?) {
    val configuration = LocalConfiguration.current
    val thumbnailMaxHeight =
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 250.dp
        else (configuration.screenHeightDp * 0.4).dp

    Thumbnail(
        modifier = Modifier.sizeIn(maxHeight = thumbnailMaxHeight),
        imageBitmap = imageBitmap,
        shape = MaterialTheme.shapes.extraSmall,
        placeholderIcon = Icons.Sharp.MusicNote,
    )
}
