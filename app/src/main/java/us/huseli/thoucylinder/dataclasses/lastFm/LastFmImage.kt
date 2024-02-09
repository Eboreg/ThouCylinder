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
        val thumbnailSizePriority = listOf(Size.LARGE, Size.EXTRALARGE, Size.MEDIUM)
    }
}

fun List<LastFmImage>.getThumbnail(): LastFmImage? =
    LastFmImage.thumbnailSizePriority.firstNotNullOfOrNull { size -> find { it.size == size && it.url.isNotEmpty() } }
