package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.toDuration
import java.util.UUID
import kotlin.time.Duration

@Parcelize
data class YoutubeVideo(
    val id: String,
    val title: String,
    val length: String? = null,
    val playlistItemId: String? = null,
    val playlistPosition: Int? = null,
    @Embedded("metadata") val metadata: YoutubeMetadata? = null,
    @Ignore val thumbnail: Image? = null,
) : Parcelable {
    constructor(
        id: String,
        title: String,
        length: String?,
        playlistItemId: String?,
        playlistPosition: Int?,
        metadata: YoutubeMetadata?,
    ) : this(id, title, length, playlistItemId, playlistPosition, metadata, null)

    @IgnoredOnParcel
    val duration: Duration?
        get() = length?.toDuration()

    override fun toString(): String = if (duration != null) "$title ($duration)" else title

    fun toTrack(
        isInLibrary: Boolean,
        albumId: UUID? = null,
        tempTrackData: TempTrackData? = null,
        image: Image? = null,
    ) = Track(
        title = title,
        isInLibrary = isInLibrary,
        youtubeVideo = this,
        image = image,
        albumPosition = playlistPosition,
        albumId = albumId,
        tempTrackData = tempTrackData,
        metadata = metadata?.toTrackMetadata(),
    )
}
