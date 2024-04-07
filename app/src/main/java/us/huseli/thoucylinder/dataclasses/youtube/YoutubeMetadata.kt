package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.dataclasses.TrackMetadata

@Parcelize
data class YoutubeMetadata(
    val mimeType: String,
    val codecs: String,
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

    private val codecList: List<String>
        get() = gson.fromJson(codecs, listType)

    private val expiresAt: Long?
        get() = url.toUri().getQueryParameter("expire")?.toLong()?.times(1000)

    val fileExtension: String
        get() = (codecList.getOrNull(0) ?: mimeType.split('/').last()).split('.').first()

    val isOld: Boolean
        get() = expiresAt?.let { it < System.currentTimeMillis() } ?: false

    val quality: Long
        get() = bitrate.toLong() * sampleRate.toLong()

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
        val VIDEO_MIMETYPE_FILTER = Regex("^audio/.*$")
        val VIDEO_MIMETYPE_EXCLUDE = null
        val VIDEO_MIMETYPE_PREFERRED = listOf("audio/opus") // most preferred first
        // val VIDEO_MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
        private val gson: Gson = GsonBuilder().create()
        private val listType = object : TypeToken<List<String>>() {}

        private fun extractCodecs(mimeType: String): String {
            val codecList = Regex("^.*codecs=\"?([^\"]*)\"?$")
                .find(mimeType)
                ?.groupValues
                ?.getOrNull(1)
                ?.split(",")
                ?: emptyList()

            return gson.toJson(codecList)
        }

        private fun extractMimeType(value: String): String {
            val mimeType = value.split(";").first()
            val codecs = extractCodecs(value)

            return if (mimeType == "audio/webm" && codecs.isNotEmpty()) "audio/${codecs.first()}"
            else mimeType
        }
    }
}


fun Iterable<YoutubeMetadata>.getBest(
    mimeTypeFilter: Regex? = YoutubeMetadata.VIDEO_MIMETYPE_FILTER,
    mimeTypeExclude: Regex? = YoutubeMetadata.VIDEO_MIMETYPE_EXCLUDE,
    preferredMimetypes: List<String> = YoutubeMetadata.VIDEO_MIMETYPE_PREFERRED,
): YoutubeMetadata? {
    // First run through the preferred MIME types, returning the best metadata if found.
    for (mimeType in preferredMimetypes) {
        filter { it.mimeType == mimeType }.maxByOrNull { it.quality }?.also { return it }
    }
    // If no metadata with preferred MIME type found, go through them all.
    return filter { mimeTypeFilter?.matches(it.mimeType) ?: true }
        .filter { mimeTypeExclude == null || !mimeTypeExclude.matches(it.mimeType) }
        .maxByOrNull { it.quality }
}
