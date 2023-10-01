@file:Suppress("unused")

package us.huseli.thoucylinder

import android.media.MediaExtractor
import java.io.File

@Suppress("unused")
class TrackDownloadException(val type: ErrorType, cause: Throwable? = null) : Exception(cause) {
    enum class ErrorType { MEDIA_STORE, EXTRACT_TRACK_DATA, DOWNLOAD, NO_METADATA, FFMPEG_CONVERT, NO_FILE }
}

open class MediaStoreException : Exception()

@Suppress("unused")
class MediaStoreFormatException(val filename: String) : MediaStoreException()

@Suppress("unused")
class ExtractTrackDataException(val file: File, val extractor: MediaExtractor) : Exception()
