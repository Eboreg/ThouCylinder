package us.huseli.thoucylinder.dataclasses

data class YoutubeMetadata(
    val mimeType: String,
    val codecs: List<String>,
    val bitrate: Int,
    val sampleRate: Int,
    val url: String,
    val size: Int? = null,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
) {
    constructor(
        mimeType: String,
        bitrate: Int,
        sampleRate: Int,
        url: String,
        size: Int? = null,
        channels: Int? = null,
        loudnessDb: Double? = null,
    ) : this(
        mimeType = mimeType.split(";").first(),
        codecs = extractCodecs(mimeType),
        bitrate = bitrate,
        sampleRate = sampleRate,
        url = url,
        size = size,
        channels = channels,
        loudnessDb = loudnessDb,
    )

    val quality: Long
        get() = bitrate.toLong() * sampleRate.toLong()

    val type: String
        get() = codecs.getOrNull(0) ?: mimeType

    companion object {
        private fun extractCodecs(mimeType: String): List<String> = Regex("^.*codecs=\"?([^\"]*)\"?$")
            .find(mimeType)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(",")
            ?: emptyList()
    }
}


data class YoutubeMetadataList(val metadata: MutableList<YoutubeMetadata> = mutableListOf()) {
    fun getBest(mimeTypeFilter: Regex? = null, mimeTypeExclude: Regex? = null): YoutubeMetadata? =
        metadata.filter { mimeTypeFilter?.matches(it.mimeType) ?: true }
            .filter { mimeTypeExclude == null || !mimeTypeExclude.matches(it.mimeType) }
            .maxByOrNull { it.quality }
}
