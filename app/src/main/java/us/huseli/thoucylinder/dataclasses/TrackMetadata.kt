package us.huseli.thoucylinder.dataclasses

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Parcelable
import android.webkit.MimeTypeMap
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.bytesToString
import us.huseli.retaintheme.formattedString
import us.huseli.thoucylinder.getIntegerOrDefault
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


fun File.extractTrackMetadata(ff: MediaInformation?): TrackMetadata {
    /** Extract metadata from audio file with MediaExtractor and ffmpeg. */
    val extractor = MediaExtractor()
    extractor.setDataSource(path)

    for (trackIdx in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(trackIdx)
        val mimeType = format.getString(MediaFormat.KEY_MIME)

        if (mimeType?.startsWith("audio/") == true) {
            val ffStream = ff?.streams?.getOrNull(trackIdx)
            val size = length()
            val extension =
                when {
                    ffStream?.codec != null && ff.format?.contains(",") == true -> ffStream.codec
                    ff?.format != null -> ff.format
                    else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        ?: mimeType.split("/").last().lowercase()
                }
            val durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000
            val bitrate = ff?.bitrate?.toInt() ?: ((size * 8) / (durationMs / 1000)).toInt()
            val channels = format.getIntegerOrDefault(
                MediaFormat.KEY_CHANNEL_COUNT,
                ffStream?.getNumberProperty("channels")?.toInt()
            )
            val sampleRate = format.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, ffStream?.sampleRate?.toInt())

            return TrackMetadata(
                bitrate = bitrate,
                durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000,
                extension = extension,
                mimeType = mimeType,
                sampleRate = sampleRate,
                channels = channels,
                size = size,
            ).also { extractor.release() }
        }
    }
    extractor.release()
    throw Exception("Could not extract metadata for $this")
}


fun File.extractTrackMetadata(): TrackMetadata =
    extractTrackMetadata(FFprobeKit.getMediaInformation(path)?.mediaInformation)
