package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.preference.PreferenceManager
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.AnyTypePermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retaintheme.extensions.join
import us.huseli.retaintheme.extensions.md5
import us.huseli.retaintheme.extensions.toHex
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SCROBBLE
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SESSION_KEY
import us.huseli.thoucylinder.Constants.PREF_LASTFM_USERNAME
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmNowPlaying
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmScrobble
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopArtistsResponse
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.fromJson
import us.huseli.thoucylinder.getMutexCache
import us.huseli.thoucylinder.interfaces.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
) : ILogger, AbstractScopeHolder() {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val albumDao = database.albumDao()
    private val scrobbleMutex = Mutex()
    private var scrobbleJob: Job? = null
    private val apiResponseCache =
        getMutexCache("LastFmRepository.apiResponseCache") { url -> Request(url).getString() }

    private val _allTopAlbumsFetched = MutableStateFlow(false)
    private val _latestNowPlaying = MutableStateFlow<LastFmNowPlaying?>(null)
    private val _nextTopAlbumPage = MutableStateFlow(1)
    private val _scrobble = MutableStateFlow(preferences.getBoolean(PREF_LASTFM_SCROBBLE, false))
    private val _scrobbles = MutableStateFlow<List<LastFmScrobble>>(emptyList())
    private val _sessionKey = MutableStateFlow(preferences.getString(PREF_LASTFM_SESSION_KEY, null))
    private val _topAlbums = MutableStateFlow<List<LastFmTopAlbumsResponse.LastFmAlbum>>(emptyList())
    private val _username = MutableStateFlow(preferences.getString(PREF_LASTFM_USERNAME, null))

    val allTopAlbumsFetched: StateFlow<Boolean> = _allTopAlbumsFetched.asStateFlow()
    val importedReleaseIds: StateFlow<List<String>> =
        albumDao.flowMusicBrainzReleaseIds().distinctUntilChanged().stateLazily(emptyList())
    val isAuthenticated = _sessionKey.map { it != null }.distinctUntilChanged()
    val scrobble: StateFlow<Boolean> = _scrobble.asStateFlow()
    val topAlbums: StateFlow<List<LastFmTopAlbumsResponse.LastFmAlbum>> = _topAlbums.asStateFlow()
    val username: StateFlow<String?> = _username.asStateFlow()

    data class Session(
        val name: String,
        val key: String,
    )

    init {
        launchOnIOThread {
            combine(_sessionKey, _scrobble) { sessionKey, scrobble -> Pair(sessionKey, scrobble) }
                .collect { (sessionKey, scrobble) ->
                    if (!scrobble || sessionKey == null) {
                        scrobbleJob?.cancel()
                        scrobbleJob = null
                    }
                    if (scrobble && sessionKey != null) {
                        scrobbleJob = launch {
                            while (true) {
                                val scrobbles = scrobbleMutex.withLock { _scrobbles.value }

                                scrobbles.chunked(50).forEach { chunk ->
                                    postAndGetString(
                                        method = "track.scrobble",
                                        params = chunk
                                            .mapIndexed { index, scrobble -> scrobble.toMap(index) }
                                            .join()
                                            .plus("sk" to sessionKey),
                                    )?.also {
                                        scrobbleMutex.withLock { _scrobbles.value -= chunk }
                                    }
                                }
                                delay(10_000)
                            }
                        }
                    }
                }
        }
    }

    suspend fun fetchNextTopAlbums(): Boolean {
        _username.value?.also { username ->
            if (!_allTopAlbumsFetched.value) {
                val url = getApiUrl(
                    mapOf(
                        "method" to "user.gettopalbums",
                        "user" to username,
                        "format" to "json",
                        "limit" to "500",
                        "page" to _nextTopAlbumPage.value.toString(),
                    )
                )

                apiResponseCache.getOrNull(url)
                    ?.fromJson<LastFmTopAlbumsResponse>()
                    ?.topalbums
                    ?.album
                    ?.filter { it.mbid.isNotEmpty() }
                    ?.also {
                        _nextTopAlbumPage.value += 1
                        _topAlbums.value += it
                        if (it.isEmpty() || _nextTopAlbumPage.value >= PAGE_LIMIT) _allTopAlbumsFetched.value = true
                        return true
                    }
            }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun fetchSession(authToken: String, enableScrobble: Boolean = true): Session? {
        val xstream = XStream()
        val body = postAndGetString(method = "auth.getSession", params = mapOf("token" to authToken))

        return body?.let {
            xstream.alias("session", Session::class.java)
            xstream.alias("lfm", List::class.java)
            xstream.ignoreUnknownElements()
            xstream.addPermission(AnyTypePermission.ANY)
            (xstream.fromXML(body) as? List<Session>)?.firstOrNull()
        }?.also { session ->
            _username.value = session.name
            _sessionKey.value = session.key

            val prefEditor = preferences
                .edit()
                .putString(PREF_LASTFM_SESSION_KEY, session.key)
                .putString(PREF_LASTFM_USERNAME, session.name)

            if (enableScrobble) {
                _scrobble.value = true
                prefEditor.putBoolean(PREF_LASTFM_SCROBBLE, true)
            }

            prefEditor.apply()
        }
    }

    suspend fun getTopArtists(limit: Int = 10): List<LastFmTopArtistsResponse.Artist> {
        return _username.value?.let { username ->
            val url = getApiUrl(
                mapOf(
                    "method" to "user.gettopartists",
                    "period" to "12month",
                    "format" to "json",
                    "user" to username,
                )
            )

            apiResponseCache.getOrNull(url)
                ?.fromJson<LastFmTopArtistsResponse>()
                ?.topartists
                ?.artist
                ?.filter { it.mbid.isNotEmpty() }
                ?.take(limit)
        } ?: emptyList()
    }

    suspend fun sendNowPlaying(combo: QueueTrackCombo, artistString: String) {
        val sessionKey = _sessionKey.value

        if (_scrobble.value && sessionKey != null) {
            val nowPlaying = LastFmNowPlaying(
                artist = artistString,
                track = combo.track.title,
                duration = combo.track.duration?.inWholeSeconds?.toInt(),
                album = combo.album?.title,
                trackNumber = combo.track.albumPosition,
                albumArtist = combo.albumArtists.joined(),
                mbid = combo.track.musicBrainzId,
            )

            if (nowPlaying != _latestNowPlaying.value) {
                postAndGetString(
                    method = "track.updateNowPlaying",
                    params = nowPlaying.toMap().plus("sk" to sessionKey)
                )
            }
            _latestNowPlaying.value = nowPlaying
        }
    }

    suspend fun sendScrobble(combo: QueueTrackCombo, artistString: String, startTimestamp: Long) {
        if (_scrobble.value) scrobbleMutex.withLock {
            _scrobbles.value += LastFmScrobble(
                track = combo.track.title,
                artist = artistString,
                album = combo.album?.title,
                albumArtist = combo.albumArtists.joined(),
                mbid = combo.track.musicBrainzId,
                trackNumber = combo.track.albumPosition,
                duration = combo.track.duration?.inWholeSeconds?.toInt(),
                timestamp = startTimestamp,
            )
        }
    }

    fun setScrobble(value: Boolean) {
        _scrobble.value = value
        preferences.edit().putBoolean(PREF_LASTFM_SCROBBLE, value).apply()
    }

    fun setUsername(value: String?) {
        if (value != _username.value) {
            _username.value = value
            _topAlbums.value = emptyList()
            _allTopAlbumsFetched.value = false
            _nextTopAlbumPage.value = 1
            preferences.edit().putString(PREF_LASTFM_USERNAME, value).apply()
        }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun getApiUrl(params: Map<String, String> = emptyMap()): String =
        Request.getUrl(API_ROOT, params.plus("api_key" to BuildConfig.lastFmApiKey))

    private fun getSignature(params: Map<String, String>): String {
        return params
            .toSortedMap()
            .map { (key, value) -> "$key$value" }
            .joinToString("")
            .plus(BuildConfig.lastFmApiSecret)
            .md5()
            .toHex()
    }

    private suspend fun postAndGetString(method: String, params: Map<String, String>): String? {
        return try {
            Request.postFormData(
                url = API_ROOT,
                formData = withSignature(
                    params.plus("method" to method).plus("api_key" to BuildConfig.lastFmApiKey)
                ),
            ).getString()
        } catch (e: Exception) {
            logError("postAndGetString $method: $e", e)
            null
        }
    }

    private fun withSignature(params: Map<String, String>): Map<String, String> =
        params.plus("api_sig" to getSignature(params))


    companion object {
        // Don't know if Last.fm has any hard quota limit, but better not overdo it:
        const val PAGE_LIMIT = 20
        const val API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    }
}
