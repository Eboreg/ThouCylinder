@file:Suppress("unused")

package us.huseli.thoucylinder

import android.media.MediaExtractor
import com.arthenica.ffmpegkit.MediaInformation
import java.io.File

class TrackDownloadException(val type: ErrorType, cause: Throwable? = null) : Exception(cause) {
    enum class ErrorType { MEDIA_STORE, EXTRACT_TRACK_DATA, DOWNLOAD, NO_STREAM_URL, FFMPEG_CONVERT }
}

class MediaStoreFormatException(val filename: String) : Exception()

class ExtractTrackDataException(val file: File, val extractor: MediaExtractor, val ff: MediaInformation?) : Exception()
