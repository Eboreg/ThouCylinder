package us.huseli.thoucylinder.data.entities

import android.webkit.MimeTypeMap
import us.huseli.thoucylinder.repositories.YoutubeRepository
import us.huseli.thoucylinder.sanitizeFilename
import kotlin.time.Duration

data class YoutubeVideo(
    val id: String,
    val title: String,
    val length: Duration? = null,
    val thumbnail: YoutubeThumbnail? = null,
) {
    private var _streamDict: YoutubeStreamDict? = null

    val streamDict: YoutubeStreamDict?
        get() = _streamDict ?: YoutubeRepository.getStreamDict(id).also { _streamDict = it }

    val streamUrl: String?
        get() = streamDict?.url

    fun generateFilename(): String {
        return streamDict?.mimeType?.split(";")?.first()?.let { mimeType ->
            val extension =
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?:
                mimeType.split("/").last().lowercase()
            sanitizeFilename("$title.$extension")
        } ?: sanitizeFilename(title)
    }

    override fun toString(): String = if (length != null) "$title ($length)" else title
}
