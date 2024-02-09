package us.huseli.thoucylinder

object Constants {
    const val COVERARTARCHIVE_API_ROOT = "https://coverartarchive.org/release"
    const val CUSTOM_USER_AGENT = "ThouCylinder/${BuildConfig.VERSION_NAME} ( https://github.com/Eboreg/ThouCylinder )"
    const val DISCOGS_API_ROOT = "https://api.discogs.com"
    const val DOWNLOAD_CHUNK_SIZE = 10 shl 16
    const val HEADER_ANDROID_SDK_VERSION = 30
    const val HEADER_X_YOUTUBE_CLIENT_NAME = "3"
    const val HEADER_X_YOUTUBE_CLIENT_VERSION = "17.31.35"
    const val IMAGE_MAX_DP_FULL = 300
    const val IMAGE_MAX_DP_THUMBNAIL = 80
    const val IMAGE_MIN_PX_THUMBNAIL = 200
    const val LASTFM_API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    const val LASTFM_AUTH_URL =
        "https://www.last.fm/api/auth/?api_key=${BuildConfig.lastFmApiKey}&cb=klaatu://${BuildConfig.hostName}/lastfm/auth"

    // Don't know if Last.fm has any hard quota limit, but better not overdo it:
    const val LASTFM_PAGE_LIMIT = 20
    const val MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
    const val MUSICBRAINZ_GENRES_URL = "$MUSICBRAINZ_API_ROOT/genre/all?fmt=txt"
    const val NAV_ARG_ALBUM = "album"
    const val NAV_ARG_ARTIST = "artist"
    const val NAV_ARG_PLAYLIST = "playlist"
    const val PREF_AUTO_IMPORT_LOCAL_MUSIC = "autoImportLocalMusic"
    const val PREF_CURRENT_TRACK_POSITION = "currentTrackPosition"
    const val PREF_LASTFM_SCROBBLE = "lastFmScrobble"
    const val PREF_LASTFM_SESSION_KEY = "lastFmSessionKey"
    const val PREF_LASTFM_USERNAME = "lastFmUsername"
    const val PREF_LOCAL_MUSIC_URI = "localMusicUri"
    const val PREF_QUEUE_INDEX = "queueIndex"
    const val PREF_SPOTIFY_ACCESS_TOKEN = "spotifyAccessToken"
    const val PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES = "spotifyAccessTokenExpires"
    const val PREF_WELCOME_DIALOG_SHOWN = "welcomeDialogShown"
    const val SPOTIFY_REDIRECT_URL = "klaatu://${BuildConfig.hostName}/spotify/import-albums"
    const val SPOTIFY_USER_ALBUMS_URL = "https://api.spotify.com/v1/me/albums"
    const val URL_CONNECT_TIMEOUT = 4_050
    const val URL_READ_TIMEOUT = 10_000
    val VALID_FILENAME_REGEX = Regex("^[^\\\\\\x00-\\x1f\"*:<>?|\\x7f]*$")
    val VIDEO_MIMETYPE_FILTER = Regex("^audio/.*$")
    val VIDEO_MIMETYPE_EXCLUDE = null
    const val YOUTUBE_BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse"
    const val YOUTUBE_USER_AGENT = "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip"
    const val YOUTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
    const val YOUTUBE_SEARCH_URL = "https://www.youtube.com/youtubei/v1/search"
    // val VIDEO_MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
}
