package us.huseli.thoucylinder.dataclasses

import android.content.ContentValues
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Parcelable
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.ExtractTrackDataException
import us.huseli.thoucylinder.bytesToString
import us.huseli.thoucylinder.formattedString
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

    val loudnessDbString: String?
        get() = loudnessDb?.formattedString(2)?.let { "$it dB" }

    val sampleRateString: String?
        get() = sampleRate?.toDouble()?.div(1000)?.formattedString(1)?.let { "$it KHz" }

    val sizeString: String?
        get() = size?.bytesToString()

    fun getContentValues() = ContentValues().apply {
        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
        put(MediaStore.Audio.Media.DURATION, durationMs.toInt())
        bitrate?.also { put(MediaStore.Audio.Media.BITRATE, it) }
    }
}


/**
 * Extract metadata from audio file with MediaExtractor and ffmpeg.
 * @throws ExtractTrackDataException
 */
fun File.extractTrackMetadata(): TrackMetadata {
    val extractor = MediaExtractor()
    extractor.setDataSource(path)

    for (trackIdx in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(trackIdx)
        val mimeType = format.getString(MediaFormat.KEY_MIME)

        if (mimeType?.startsWith("audio/") == true) {
            val ff = FFprobeKit.getMediaInformation(path)?.mediaInformation
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
    throw ExtractTrackDataException(this, extractor)
}
