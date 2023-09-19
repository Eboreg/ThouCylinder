package us.huseli.thoucylinder.data.entities

data class YoutubeStreamDict(
    private val _mimeType: String,
    val bitrate: Int,
    val sampleRate: Int,
    val url: String,
    val size: Int? = null,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
) {
    private val codecs: List<String>
        get() = Regex("^.*codecs=\"?([^\"]*)\"?$")
            .find(_mimeType)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(",")
            ?: emptyList()

    val quality: Long
        get() = bitrate.toLong() * sampleRate.toLong()

    val mimeType: String
        get() = _mimeType.split(";").first()

    val type: String
        get() = codecs.getOrNull(0) ?: mimeType
}


data class YoutubeStreamData(val streamDicts: MutableList<YoutubeStreamDict> = mutableListOf()) {
    fun getBestStreamDict(mimeTypeFilter: Regex? = null, mimeTypeExclude: Regex? = null): YoutubeStreamDict? =
        streamDicts.filter { mimeTypeFilter?.matches(it.mimeType) ?: true }
            .filter { mimeTypeExclude == null || !mimeTypeExclude.matches(it.mimeType) }
            .maxByOrNull { it.quality }
}
