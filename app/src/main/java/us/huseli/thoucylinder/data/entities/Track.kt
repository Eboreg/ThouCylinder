package us.huseli.thoucylinder.data.entities

import android.net.Uri
import us.huseli.retaintheme.sensibleFormat
import java.util.UUID
import kotlin.time.Duration

data class Track(
    val title: String,
    val id: UUID = UUID.randomUUID(),
    val artist: String? = null,
    val youtubeVideo: YoutubeVideo? = null,
    val youtubeThumbnail: YoutubeThumbnail? = null,
    val localUri: Uri? = null,
    val length: Duration? = null,
) {
    override fun toString(): String =
        (artist?.let { "$artist - " } ?: "") + title + (length?.let { " (${length.sensibleFormat()})" } ?: "")
}
