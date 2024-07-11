package us.huseli.thoucylinder.dataclasses.lastfm

import com.google.gson.annotations.SerializedName
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.toMediaStoreImage

data class LastFmImage(
    val size: Size,
    @SerializedName("#text") val url: String,
) {
    @Suppress("unused")
    enum class Size {
        @SerializedName("small") SMALL,
        @SerializedName("medium") MEDIUM,
        @SerializedName("large") LARGE,
        @SerializedName("extralarge") EXTRALARGE,
        @SerializedName("mega") MEGA,
    }

    companion object {
        val fullImageSizePriority = listOf(Size.MEGA, Size.EXTRALARGE, Size.LARGE)
        val thumbnailSizePriority = listOf(Size.LARGE, Size.EXTRALARGE, Size.MEDIUM)
    }
}

private fun Iterable<LastFmImage>.getImage(sizePriority: List<LastFmImage.Size>): LastFmImage? =
    sizePriority.firstNotNullOfOrNull { size -> find { it.size == size && it.url.isNotEmpty() } }

private fun Iterable<LastFmImage>.getFullImage(): LastFmImage? = getImage(LastFmImage.fullImageSizePriority)

fun Iterable<LastFmImage>.getThumbnail(): LastFmImage? = getImage(LastFmImage.thumbnailSizePriority)

fun Iterable<LastFmImage>.toMediaStoreImage(): MediaStoreImage? =
    getFullImage()?.url?.toMediaStoreImage(getThumbnail()?.url)
