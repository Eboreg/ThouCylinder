package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.toDuration
import java.io.File
import kotlin.time.Duration

@Parcelize
data class YoutubeVideo(
    val id: String,
    val title: String,
    val length: String? = null,
    @Ignore val thumbnail: Image? = null,
) : Parcelable {
    constructor(id: String, title: String, length: String) :
        this(id, title, length, null)

    @Ignore
    @IgnoredOnParcel
    val duration: Duration? = length?.toDuration()

    override fun toString(): String = if (duration != null) "$title ($duration)" else title

    fun toTempTrack(localFile: File, metadata: TrackMetadata) = TempTrack(
        title = title,
        metadata = metadata,
        localFile = localFile,
        youtubeVideo = this,
        image = thumbnail,
    )
}
