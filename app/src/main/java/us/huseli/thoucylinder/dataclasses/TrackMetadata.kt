package us.huseli.thoucylinder.dataclasses

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.formattedString
import us.huseli.thoucylinder.Logger
import us.huseli.thoucylinder.getIntegerOrDefault
import us.huseli.thoucylinder.getLongOrNull
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
@Immutable
data class TrackMetadata(
    val durationMs: Long,
    val mimeType: String,
    val bitrate: Int? = null,
    val channels: Int? = null,
    val loudnessDb: Double? = null,
    val sampleRate: Int? = null,
) : Parcelable {
    val bitrateString: String?
        get() = bitrate?.div(1000)?.let { "$it Kbps" }

    val duration: Duration
        get() = durationMs.milliseconds

    val sampleRateString: String?
        get() = sampleRate?.toDouble()?.div(1000)?.formattedString(1)?.let { "$it KHz" }
}


fun MediaExtractor.extractTrackMetadata(ff: MediaInformation?): TrackMetadata {
    for (trackIdx in 0 until trackCount) {
        val format = getTrackFormat(trackIdx)
        val mimeType = format.getString(MediaFormat.KEY_MIME)
        val ffStream = ff?.streams?.getOrNull(trackIdx)

        if (mimeType?.startsWith("audio/") == true) {
            return TrackMetadata(
                bitrate = ff?.bitrate?.toInt(),
                channels = format.getIntegerOrDefault(
                    MediaFormat.KEY_CHANNEL_COUNT,
                    ffStream?.getNumberProperty("channels")?.toInt()
                ),
                durationMs = format.getLongOrNull(MediaFormat.KEY_DURATION)?.div(1000)
                    ?: ff?.duration?.toFloat()?.times(1000)?.toLong() ?: 0L,
                mimeType = mimeType,
                sampleRate = format.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, ffStream?.sampleRate?.toInt()),
            ).also { release() }
        }
    }

    release()
    throw Exception("Could not extract metadata for $this")
}


fun File.extractTrackMetadata(ff: MediaInformation?): TrackMetadata? {
    /** Extract metadata from audio file with MediaExtractor and ffmpeg. */
    return try {
        val extractor = MediaExtractor().also { it.setDataSource(path) }
        extractor.extractTrackMetadata(ff).also { extractor.release() }
    } catch (e: Exception) {
        Logger.logError("File", "extractTrackMetadata", e)
        null
    }
}


fun File.extractTrackMetadata(): TrackMetadata? =
    extractTrackMetadata(FFprobeKit.getMediaInformation(path)?.mediaInformation)
