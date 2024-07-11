package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.room.Ignore
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.ContainerFormat
import us.huseli.thoucylinder.dataclasses.track.TrackMetadata

@Parcelize
@Immutable
data class YoutubeMetadata(
    val mimeType: String,
    val bitrate: Int,
    val sampleRate: Int,
    val url: String,
    val size: Int,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
    val durationMs: Long? = null,
    val lofiUrl: String? = null,
    @Ignore val containerFormat: ContainerFormat = ContainerFormat(mimeType),
) : Parcelable {
    constructor(
        mimeType: String,
        bitrate: Int,
        sampleRate: Int,
        url: String,
        size: Int,
        channels: Int?,
        loudnessDb: Double?,
        durationMs: Long?,
        lofiUrl: String?,
    ) : this(
        mimeType = mimeType,
        bitrate = bitrate,
        sampleRate = sampleRate,
        url = url,
        size = size,
        channels = channels,
        loudnessDb = loudnessDb,
        durationMs = durationMs,
        lofiUrl = lofiUrl,
        containerFormat = ContainerFormat(mimeType),
    )

    val fileExtension: String
        get() = mimeType.split(";").first().split('/').last()

    val urlIsOld: Boolean
        get() = urlIsOld(url)

    val lofiUrlIsOld: Boolean
        get() = urlIsOld(lofiUrl)

    val quality: Long
        get() = bitrate.toLong() * sampleRate.toLong()

    fun toTrackMetadata(): TrackMetadata {
        return TrackMetadata(
            durationMs = durationMs ?: 0L,
            mimeType = containerFormat.audioMimeType,
            bitrate = bitrate,
            channels = channels,
            loudnessDb = loudnessDb,
            sampleRate = sampleRate,
        )
    }

    private fun urlExpiresAt(url: String?): Long? = url?.toUri()?.getQueryParameter("expire")?.toLong()?.times(1000)

    private fun urlIsOld(url: String?): Boolean = urlExpiresAt(url)?.let { it < System.currentTimeMillis() } ?: false

    companion object {
        val VIDEO_MIMETYPE_FILTER = Regex("^audio/.*$")
        val VIDEO_MIMETYPE_EXCLUDE = null
        val VIDEO_MIMETYPE_PREFERRED = listOf(
            // most preferred first
            Regex("^audio/webm.*codecs=\".*opus.*\"$"),
        )
        // val VIDEO_MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
    }
}


fun Iterable<YoutubeMetadata>.getBest(
    mimeTypeFilter: Regex? = YoutubeMetadata.VIDEO_MIMETYPE_FILTER,
    mimeTypeExclude: Regex? = YoutubeMetadata.VIDEO_MIMETYPE_EXCLUDE,
    preferredMimetypes: List<Regex> = YoutubeMetadata.VIDEO_MIMETYPE_PREFERRED,
): YoutubeMetadata? {
    val lofiUrl = filter { mimeTypeFilter?.matches(it.mimeType) ?: true }.minByOrNull { it.size }?.url

    // Run through the preferred MIME types, returning the best metadata if found:
    for (mimeType in preferredMimetypes) {
        filter { mimeType.matches(it.mimeType) }.maxByOrNull { it.quality }?.also { return it.copy(lofiUrl = lofiUrl) }
    }
    // If no metadata with preferred MIME type found, go through them all:
    return filter { mimeTypeFilter?.matches(it.mimeType) ?: true }
        .filter { mimeTypeExclude == null || !mimeTypeExclude.matches(it.mimeType) }
        .maxByOrNull { it.quality }
        ?.copy(lofiUrl = lofiUrl)
}
