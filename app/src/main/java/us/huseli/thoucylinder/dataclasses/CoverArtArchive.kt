package us.huseli.thoucylinder.dataclasses

import com.google.gson.annotations.SerializedName

@Suppress("unused")
enum class CoverArtArchiveImageType {
    @SerializedName("Back") BACK,
    @SerializedName("Booklet") BOOKLET,
    @SerializedName("Bottom") BOTTOM,
    @SerializedName("Front") FRONT,
    @SerializedName("Liner") LINER,
    @SerializedName("Matrix/Runout") MATRIX_RUNOUT,
    @SerializedName("Medium") MEDIUM,
    @SerializedName("Obi") OBI,
    @SerializedName("Other") OTHER,
    @SerializedName("Poster") POSTER,
    @SerializedName("Raw/Unedited") RAW_UNEDITED,
    @SerializedName("Spine") SPINE,
    @SerializedName("Sticker") STICKER,
    @SerializedName("Top") TOP,
    @SerializedName("Track") TRACK,
    @SerializedName("Tray") TRAY,
    @SerializedName("Watermark") WATERMARK,
}

data class CoverArtArchiveImageThumbnails(
    @SerializedName("250") val thumb250: String,
    @SerializedName("500") val thumb500: String,
    @SerializedName("1200") val thumb1200: String,
)

data class CoverArtArchiveImage(
    val approved: Boolean,
    val back: Boolean,
    val comment: String,
    val edit: Int,
    val front: Boolean,
    val id: Long,
    val image: String,
    val thumbnails: CoverArtArchiveImageThumbnails,
    val types: List<CoverArtArchiveImageType>,
)

data class CoverArtArchiveResponse(
    val images: List<CoverArtArchiveImage>,
    val release: String,
)
