package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.formattedString
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
    @Ignore
    @IgnoredOnParcel
    val duration = durationMs.milliseconds

    @Ignore
    @IgnoredOnParcel
    val audioFormat: String = mimeType.split("/").last() +
        (sampleRate?.toDouble()?.formattedString(1)?.let { " / $it KHz" } ?: "") +
        (bitrate?.div(1000)?.let { " / $it Kbs" } ?: "")
}
