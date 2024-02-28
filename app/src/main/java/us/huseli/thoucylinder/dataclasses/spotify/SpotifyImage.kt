package us.huseli.thoucylinder.dataclasses.spotify

import us.huseli.retaintheme.extensions.pow
import us.huseli.thoucylinder.Constants.IMAGE_MIN_PX_THUMBNAIL
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import kotlin.math.min

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?,
) {
    val size: Int
        get() = min(width ?: 0, height ?: 0).pow(2)
}

fun Collection<SpotifyImage>.getThumbnailUrl(): String? =
    filter { it.size >= IMAGE_MIN_PX_THUMBNAIL }.minByOrNull { it.size }?.url

fun Collection<SpotifyImage>.getFullImageUrl(): String? = maxByOrNull { it.size }?.url

fun Collection<SpotifyImage>.toMediaStoreImage() =
    getFullImageUrl()?.let { MediaStoreImage.fromUrls(it, getThumbnailUrl()) }
