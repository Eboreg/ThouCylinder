package us.huseli.thoucylinder.repositories

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.preference.PreferenceManager
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.AnyTypePermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retaintheme.extensions.join
import us.huseli.retaintheme.extensions.md5
import us.huseli.retaintheme.extensions.toHex
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SCROBBLE
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SESSION_KEY
import us.huseli.thoucylinder.Constants.PREF_LASTFM_USERNAME
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.asFullImageBitmap
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmNowPlaying
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmScrobble
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.getThumbnail
import us.huseli.thoucylinder.fromJson
import us.huseli.thoucylinder.getBitmapByUrl
import us.huseli.thoucylinder.getMutexCache
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class LastFmRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
    playerRepo: PlayerRepository,
) : PlayerRepositoryListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val albumDao = database.albumDao()
    private val scrobbleMutex = Mutex()
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scrobbleJob: Job? = null
    private val apiResponseCache =
        getMutexCache("LastFmRepository.apiResponseCache") { url -> Request(url).getString() }

    private val _allTopAlbumsFetched = MutableStateFlow(false)
    private val _topAlbums = MutableStateFlow<List<LastFmTopAlbumsResponse.Album>>(emptyList())
    private val _nextTopAlbumPage = MutableStateFlow(1)
    private val _username = MutableStateFlow(preferences.getString(PREF_LASTFM_USERNAME, null))
    private val _sessionKey = MutableStateFlow(preferences.getString(PREF_LASTFM_SESSION_KEY, null))
    private val _scrobbles = MutableStateFlow<List<LastFmScrobble>>(emptyList())
    private val _latestNowPlaying = MutableStateFlow<LastFmNowPlaying?>(null)
    private val _scrobble = MutableStateFlow(preferences.getBoolean(PREF_LASTFM_SCROBBLE, false))

    val allTopAlbumsFetched: StateFlow<Boolean> = _allTopAlbumsFetched.asStateFlow()
    val topAlbums: StateFlow<List<LastFmTopAlbumsResponse.Album>> = _topAlbums.asStateFlow()
    val username: StateFlow<String?> = _username.asStateFlow()
    val scrobble: StateFlow<Boolean> = _scrobble.asStateFlow()

    data class Session(
        val name: String,
        val key: String,
    )

    init {
        playerRepo.addListener(this)

        scope.launch {
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
    suspend fun fetchSession(authToken: String): Session? {
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
            _scrobble.value = true
            preferences
                .edit()
                .putString(PREF_LASTFM_SESSION_KEY, session.key)
                .putString(PREF_LASTFM_USERNAME, session.name)
                .putBoolean(PREF_LASTFM_SCROBBLE, true)
                .apply()
        }
    }

    suspend fun getThumbnail(album: LastFmTopAlbumsResponse.Album): ImageBitmap? =
        album.image.getThumbnail()?.let { image -> image.url.getBitmapByUrl()?.asFullImageBitmap(context) }

    suspend fun listImportedAlbumIds(): List<String> = albumDao.listMusicBrainzReleaseIds()

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

    /** PRIVATE METHODS *******************************************************/
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
            Log.e(javaClass.simpleName, "postAndGetString $method: $e", e)
            null
        }
    }

    private fun withSignature(params: Map<String, String>): Map<String, String> =
        params.plus("api_sig" to getSignature(params))


    /** OVERRIDDEN METHODS ****************************************************/
    override suspend fun onPlaybackChange(combo: QueueTrackCombo?, state: PlayerRepository.PlaybackState) {
        _sessionKey.value?.also { sessionKey ->
            if (_scrobble.value) {
                val nowPlaying = combo?.let {
                    it.artists.joined()?.let { artist ->
                        LastFmNowPlaying(
                            artist = artist,
                            track = it.track.title,
                            duration = it.track.duration?.inWholeSeconds?.toInt(),
                            album = it.album?.title,
                            trackNumber = it.track.albumPosition,
                            albumArtist = it.albumArtist,
                            mbid = it.track.musicBrainzId,
                        )
                    }
                }

                if (nowPlaying != null && nowPlaying != _latestNowPlaying.value) {
                    postAndGetString(
                        method = "track.updateNowPlaying",
                        params = nowPlaying.toMap().plus("sk" to sessionKey)
                    )
                }
                _latestNowPlaying.value = nowPlaying
            }
        }
    }

    override suspend fun onHalfTrackPlayed(combo: QueueTrackCombo, startTimestamp: Long) {
        val artist = combo.artists.joined()
        val duration = combo.track.duration

        if (artist != null && duration != null && duration > 30.seconds && _scrobble.value) {
            scrobbleMutex.withLock {
                _scrobbles.value += LastFmScrobble(
                    track = combo.track.title,
                    artist = artist,
                    album = combo.album?.title,
                    albumArtist = combo.albumArtist,
                    mbid = combo.track.musicBrainzId,
                    trackNumber = combo.track.albumPosition,
                    duration = combo.track.duration?.inWholeSeconds?.toInt(),
                    timestamp = startTimestamp,
                )
            }
        }
    }

    companion object {
        // Don't know if Last.fm has any hard quota limit, but better not overdo it:
        const val PAGE_LIMIT = 20
        const val API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    }
}
