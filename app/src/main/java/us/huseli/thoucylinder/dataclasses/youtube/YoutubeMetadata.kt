package us.huseli.thoucylinder.dataclasses.youtube

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.TrackMetadata

@Parcelize
data class YoutubeMetadata(
    val mimeType: String,
    val codecs: List<String>,
    val bitrate: Int,
    val sampleRate: Int,
    val url: String,
    val size: Int? = null,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
    val durationMs: Long? = null,
) : Parcelable {
    constructor(
        mimeType: String,
        bitrate: Int,
        sampleRate: Int,
        url: String,
        size: Int? = null,
        channels: Int? = null,
        loudnessDb: Double? = null,
        durationMs: Long? = null,
    ) : this(
        mimeType = extractMimeType(mimeType),
        codecs = extractCodecs(mimeType),
        bitrate = bitrate,
        sampleRate = sampleRate,
        url = url,
        size = size,
        channels = channels,
        loudnessDb = loudnessDb,
        durationMs = durationMs,
    )

    private val expiresAt: Long?
        get() = uri.getQueryParameter("expire")?.toLong()?.times(1000)

    val fileExtension: String
        get() = (codecs.getOrNull(0) ?: mimeType.split('/').last()).split('.').first()

    val isOld: Boolean
        get() = expiresAt?.let { it < System.currentTimeMillis() } ?: false

    val quality: Long
        get() = bitrate.toLong() * sampleRate.toLong()

    val uri: Uri
        get() = url.toUri()

    fun toTrackMetadata() = TrackMetadata(
        durationMs = durationMs ?: 0L,
        extension = fileExtension,
        mimeType = mimeType,
        bitrate = bitrate,
        channels = channels,
        loudnessDb = loudnessDb,
        sampleRate = sampleRate,
        size = size?.toLong(),
    )

    companion object {
        private fun extractCodecs(mimeType: String): List<String> = Regex("^.*codecs=\"?([^\"]*)\"?$")
            .find(mimeType)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(",")
            ?: emptyList()

        private fun extractMimeType(value: String): String {
            val mimeType = value.split(";").first()
            val codecs = extractCodecs(value)

            return if (mimeType == "audio/webm" && codecs.isNotEmpty()) "audio/${codecs.first()}"
            else mimeType
        }
    }
}


data class YoutubeMetadataList(val metadata: MutableList<YoutubeMetadata> = mutableListOf()) {
    fun getBest(
        mimeTypeFilter: Regex? = null,
        mimeTypeExclude: Regex? = null,
        preferredMimetypes: List<String> = emptyList(),
    ): YoutubeMetadata? {
        // First run through the preferred MIME types, returning the best metadata if found.
        for (mimeType in preferredMimetypes) {
            metadata.filter { it.mimeType == mimeType }.maxByOrNull { it.quality }?.also { return it }
        }
        // If no metadata with preferred MIME type found, go through them all.
        return metadata
            .filter { mimeTypeFilter?.matches(it.mimeType) ?: true }
            .filter { mimeTypeExclude == null || !mimeTypeExclude.matches(it.mimeType) }
            .maxByOrNull { it.quality }
    }
}
