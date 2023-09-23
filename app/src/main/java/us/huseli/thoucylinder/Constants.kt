package us.huseli.thoucylinder

object Constants {
    const val DOWNLOAD_CHUNK_SIZE = 10 shl 16
    const val HEADER_USER_AGENT = "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip"
    const val HEADER_X_YOUTUBE_CLIENT_NAME = "3"
    const val HEADER_X_YOUTUBE_CLIENT_VERSION = "17.31.35"
    const val HEADER_ANDROID_SDK_VERSION = 30
    const val NAV_ARG_ALBUM = "album"
    const val NAV_ARG_PLAYLIST = "playlist"
    const val URL_CONNECT_TIMEOUT = 4_050
    const val URL_READ_TIMEOUT = 10_000
    val VIDEO_MIMETYPE_FILTER = Regex("^audio/.*$")
    val VIDEO_MIMETYPE_EXCLUDE = null
    // val VIDEO_MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
}
