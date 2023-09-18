package us.huseli.thoucylinder.data.entities

import android.net.Uri
import us.huseli.thoucylinder.repositories.YoutubeRepository
import us.huseli.thoucylinder.sanitizeFilename
import kotlin.time.Duration

data class YoutubeVideo(
    val id: String,
    val title: String,
    val length: Duration? = null,
    val thumbnail: YoutubeThumbnail? = null,
    var localUri: Uri? = null,
) {
    private var _streamData: YoutubeStreamData? = null

    fun generateBasename(trackNumber: Int? = null): String =
        ((trackNumber?.let { String.format("%02d", it) + " - " } ?: "") + title).sanitizeFilename()

    fun getBestStreamDict(): YoutubeStreamDict? =
        (_streamData ?: YoutubeRepository.getStreamData(id).also { _streamData = it })
            .getBestStreamDict(MIMETYPE_FILTER, MIMETYPE_EXCLUDE)

    fun toTrack(artist: String? = null): Track = Track(
        title = title,
        artist = artist,
        youtubeVideo = this,
        localUri = localUri,
        length = length,
        youtubeThumbnail = thumbnail,
    )

    override fun toString(): String = if (length != null) "$title ($length)" else title

    companion object {
        val MIMETYPE_FILTER = Regex("^audio/.*$")
        val MIMETYPE_EXCLUDE = null
        // val MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
    }
}
