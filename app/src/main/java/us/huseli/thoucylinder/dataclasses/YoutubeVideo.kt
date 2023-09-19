package us.huseli.thoucylinder.dataclasses

import androidx.room.Ignore
import androidx.room.TypeConverters
import us.huseli.thoucylinder.database.Converters
import us.huseli.thoucylinder.sanitizeFilename
import us.huseli.thoucylinder.toDuration
import java.util.UUID
import kotlin.time.Duration

@TypeConverters(Converters::class)
data class YoutubeVideo(
    val id: String,
    val title: String,
    val length: String? = null,
    @Ignore val thumbnail: Image? = null,
) {
    constructor(id: String, title: String, length: String) :
        this(id, title, length, null)

    @Ignore
    val duration: Duration? = length?.toDuration()

    fun generateBasename(trackNumber: Int? = null): String =
        ((trackNumber?.let { String.format("%02d", it) + " - " } ?: "") + title).sanitizeFilename()

    fun toTrack(
        localPath: String,
        artist: String? = null,
        albumId: UUID? = null,
        albumPosition: Int? = null,
    ): Track = Track(
        title = title,
        artist = artist,
        youtubeVideo = this,
        localPath = localPath,
        length = length,
        image = thumbnail,
        albumId = albumId,
        albumPosition = albumPosition,
    )

    override fun toString(): String = if (duration != null) "$title ($duration)" else title
}
