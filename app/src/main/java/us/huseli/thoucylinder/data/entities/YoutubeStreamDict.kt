package us.huseli.thoucylinder.data.entities

data class YoutubeStreamDict(
    val mimeType: String,
    val bitrate: Int,
    val sampleRate: Int,
    val url: String,
) {
    val quality: Int
        get() = bitrate * sampleRate
}
