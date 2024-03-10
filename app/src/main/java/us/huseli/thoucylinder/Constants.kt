package us.huseli.thoucylinder

object Constants {
    const val CUSTOM_USER_AGENT = "ThouCylinder/${BuildConfig.VERSION_NAME} ( https://github.com/Eboreg/ThouCylinder )"
    const val IMAGE_MAX_DP_FULL = 300
    const val IMAGE_MAX_DP_THUMBNAIL = 80
    const val IMAGE_MIN_PX_THUMBNAIL = 200
    const val LASTFM_AUTH_URL =
        "https://www.last.fm/api/auth/?api_key=${BuildConfig.lastFmApiKey}&cb=klaatu://${BuildConfig.hostName}/lastfm/auth"
    const val NAV_ARG_ALBUM = "album"
    const val NAV_ARG_ARTIST = "artist"
    const val NAV_ARG_PLAYLIST = "playlist"
    const val PREF_ACTIVE_RADIO_ID = "activeRadioId"
    const val PREF_AUTO_IMPORT_LOCAL_MUSIC = "autoImportLocalMusic"
    const val PREF_CURRENT_TRACK_POSITION = "currentTrackPosition"
    const val PREF_LASTFM_SCROBBLE = "lastFmScrobble"
    const val PREF_LASTFM_SESSION_KEY = "lastFmSessionKey"
    const val PREF_LASTFM_USERNAME = "lastFmUsername"
    const val PREF_LOCAL_MUSIC_URI = "localMusicUri"
    const val PREF_QUEUE_INDEX = "queueIndex"
    const val PREF_SPOTIFY_OAUTH2_TOKEN = "spotifyOAuth2Token"
    const val PREF_SPOTIFY_CODE_VERIFIER = "spotifyCodeVerifier"
    const val PREF_WELCOME_DIALOG_SHOWN = "welcomeDialogShown"
    const val PREF_UMLAUTIFY = "umlautify"
}
