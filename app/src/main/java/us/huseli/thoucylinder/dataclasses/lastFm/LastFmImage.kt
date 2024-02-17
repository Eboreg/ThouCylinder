package us.huseli.thoucylinder.dataclasses.lastFm

import com.google.gson.annotations.SerializedName

data class LastFmImage(
    val size: Size,
    @SerializedName("#text")
    val url: String,
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


fun List<LastFmImage>.getImage(sizePriority: List<LastFmImage.Size>): LastFmImage? =
    sizePriority.firstNotNullOfOrNull { size -> find { it.size == size && it.url.isNotEmpty() } }

fun List<LastFmImage>.getFullImage(): LastFmImage? = getImage(LastFmImage.fullImageSizePriority)


fun List<LastFmImage>.getThumbnail(): LastFmImage? = getImage(LastFmImage.thumbnailSizePriority)
