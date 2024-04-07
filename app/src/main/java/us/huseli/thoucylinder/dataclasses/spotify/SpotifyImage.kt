package us.huseli.thoucylinder.dataclasses.spotify

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MIN_WIDTH_PX
import us.huseli.thoucylinder.asThumbnailImageBitmap
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.getBitmap
import kotlin.math.min

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?,
) {
    val shortestSide: Int
        get() = min(width ?: 0, height ?: 0)
}

private fun Collection<SpotifyImage>.getThumbnailUrl(): String? =
    filter { it.shortestSide >= IMAGE_THUMBNAIL_MIN_WIDTH_PX }.minByOrNull { it.shortestSide }?.url

suspend fun Collection<SpotifyImage>.getThumbnailImageBitmap(context: Context): ImageBitmap? =
    getThumbnailUrl()?.toUri()?.getBitmap(context)?.asThumbnailImageBitmap(context)

fun Collection<SpotifyImage>.toMediaStoreImage() =
    maxByOrNull { it.shortestSide }?.url?.let { MediaStoreImage.fromUrls(it, getThumbnailUrl()) }
