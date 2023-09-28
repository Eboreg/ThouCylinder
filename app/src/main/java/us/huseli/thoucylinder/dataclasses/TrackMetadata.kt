package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.bytesToString
import us.huseli.thoucylinder.formattedString
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

    val audioFormat: String
        get() = mimeType.split("/").last() +
            (sampleRateString?.let { " / $it" } ?: "") +
            (bitrateString?.let { " / $it" } ?: "")
}
