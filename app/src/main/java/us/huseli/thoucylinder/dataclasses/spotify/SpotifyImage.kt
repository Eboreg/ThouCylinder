package us.huseli.thoucylinder.dataclasses.spotify

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MIN_WIDTH_PX
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import us.huseli.thoucylinder.getCachedThumbnailBitmap
import kotlin.math.min

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?,
) {
    val shortestSide: Int
        get() = min(width ?: 0, height ?: 0)
}

fun Collection<SpotifyImage>.getThumbnailUrl(): String? =
    filter { it.shortestSide >= IMAGE_THUMBNAIL_MIN_WIDTH_PX }.minByOrNull { it.shortestSide }?.url

suspend fun Collection<SpotifyImage>.getThumbnailImageBitmap(context: Context): ImageBitmap? =
    getThumbnailUrl()?.toUri()?.getCachedThumbnailBitmap(context)?.asImageBitmap()

suspend fun Collection<SpotifyImage>.toMediaStoreImage() =
    maxByOrNull { it.shortestSide }?.url?.toMediaStoreImage(getThumbnailUrl())
