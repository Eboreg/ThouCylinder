package us.huseli.thoucylinder.data.entities

data class YoutubeThumbnail(
    val url: String,
    val width: Int,
    val height: Int,
) {
    val size: Int
        get() = width * height
}
