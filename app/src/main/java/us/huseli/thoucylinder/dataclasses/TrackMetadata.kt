package us.huseli.thoucylinder.dataclasses

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Parcelable
import android.util.Log
import android.webkit.MimeTypeMap
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.bytesToString
import us.huseli.retaintheme.formattedString
import us.huseli.thoucylinder.getIntegerOrDefault
import us.huseli.thoucylinder.getLongOrNull
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class TrackMetadata(
    val durationMs: Long,
    val extension: String,
    val mimeType: String,
    val bitrate: Int? = null,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
    val sampleRate: Int? = null,
    val size: Long? = null,
) : Parcelable {
    val bitrateString: String?
        get() = bitrate?.div(1000)?.let { "$it Kbps" }

    val duration: Duration
        get() = durationMs.milliseconds

    val sampleRateString: String?
        get() = sampleRate?.toDouble()?.div(1000)?.formattedString(1)?.let { "$it KHz" }

    val sizeString: String?
        get() = size?.bytesToString()
}


fun MediaExtractor.extractTrackMetadata(ff: MediaInformation?): TrackMetadata {
    for (trackIdx in 0 until trackCount) {
        val format = getTrackFormat(trackIdx)
        val mimeType = format.getString(MediaFormat.KEY_MIME)
        val ffStream = ff?.streams?.getOrNull(trackIdx)

        if (mimeType?.startsWith("audio/") == true) {
            val metadata = TrackMetadata(
                bitrate = format.getIntegerOrDefault(MediaFormat.KEY_BIT_RATE, ff?.bitrate?.toInt()),
                channels = format.getIntegerOrDefault(
                    MediaFormat.KEY_CHANNEL_COUNT,
                    ffStream?.getNumberProperty("channels")?.toInt()
                ),
                // durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000,
                durationMs = format.getLongOrNull(MediaFormat.KEY_DURATION)?.div(1000)
                    ?: ff?.duration?.toFloat()?.times(1000)?.toLong() ?: 0L,
                extension = when {
                    ffStream?.codec != null && ff.format?.contains(",") == true -> ffStream.codec
                    ff?.format != null -> ff.format
                    else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        ?: mimeType.split("/").last().lowercase()
                },
                mimeType = mimeType,
                sampleRate = format.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, ffStream?.sampleRate?.toInt()),
            ).also { release() }

            return metadata
        }
    }

    release()
    throw Exception("Could not extract metadata for $this")
}


fun File.extractTrackMetadata(ff: MediaInformation?): TrackMetadata? {
    /** Extract metadata from audio file with MediaExtractor and ffmpeg. */
    return try {
        val extractor = MediaExtractor().also { it.setDataSource(path) }
        val metadata = extractor.extractTrackMetadata(ff)

        metadata.copy(size = length()).also { extractor.release() }
    } catch (e: Exception) {
        Log.e("File", "extractTrackMetadata", e)
        null
    }
}


fun File.extractTrackMetadata(): TrackMetadata? =
    extractTrackMetadata(FFprobeKit.getMediaInformation(path)?.mediaInformation)
