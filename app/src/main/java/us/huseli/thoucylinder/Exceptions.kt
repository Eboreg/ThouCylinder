package us.huseli.thoucylinder

import android.content.ContentValues
import android.media.MediaExtractor
import com.arthenica.ffmpegkit.MediaInformation
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import java.io.File

class TrackDownloadException(
    val video: YoutubeVideo,
    val type: ErrorType,
    cause: Throwable? = null,
) : Exception(cause) {
    enum class ErrorType { MEDIA_STORE, EXTRACT_TRACK_DATA, DOWNLOAD, NO_STREAM_URL, FFMPEG_CONVERT }
}

class MediaStoreFormatException(val filename: String, val trackDetails: ContentValues) : Exception()

class ExtractTrackDataException(val file: File, val extractor: MediaExtractor, val ff: MediaInformation?) : Exception()
