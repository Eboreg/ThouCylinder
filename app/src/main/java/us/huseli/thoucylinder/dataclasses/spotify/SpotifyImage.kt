package us.huseli.thoucylinder.dataclasses.spotify

import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MIN_WIDTH_PX
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage
import kotlin.math.min

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?,
) {
    val shortestSide: Int
        get() = min(width ?: 0, height ?: 0)
}

fun Iterable<SpotifyImage>.getThumbnailUrl(): String? =
    filter { it.shortestSide >= IMAGE_THUMBNAIL_MIN_WIDTH_PX }.minByOrNull { it.shortestSide }?.url

fun Iterable<SpotifyImage>.toMediaStoreImage() =
    maxByOrNull { it.shortestSide }?.url?.toMediaStoreImage(getThumbnailUrl())
