package us.huseli.thoucylinder.data.entities

data class ExtractedTrackData(
    val mimeType: String,
    val durationUs: Long,
    val extension: String,
    val codec: String? = null,
    val ffFormat: String? = null,
    val ffCodec: String? = null,
)
