package us.huseli.thoucylinder.dataclasses.lastFm

import com.google.gson.annotations.SerializedName

data class LastFmImage(
    val size: String,
    @SerializedName("#text")
    val url: String,
) {
    companion object {
        val fullImageSizePriority = listOf("mega", "extralarge", "large", "medium")
        val thumbnailSizePriority = listOf("large", "extralarge", "medium")
    }
}

fun List<LastFmImage>.getThumbnail(): LastFmImage? =
    LastFmImage.thumbnailSizePriority.firstNotNullOfOrNull { size -> find { it.size == size && it.url.isNotEmpty() } }

fun List<LastFmImage>.getFullImage(): LastFmImage? =
    LastFmImage.fullImageSizePriority.firstNotNullOfOrNull { size -> find { it.size == size && it.url.isNotEmpty() } }
