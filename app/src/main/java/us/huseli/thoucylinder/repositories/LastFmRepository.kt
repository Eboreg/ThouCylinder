package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import us.huseli.thoucylinder.Constants.LASTFM_API_ROOT
import us.huseli.thoucylinder.Constants.LASTFM_PAGE_LIMIT
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SCROBBLE
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SESSION_KEY
import us.huseli.thoucylinder.Constants.PREF_LASTFM_USERNAME
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmNowPlaying
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmScrobble
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.getLastFmTopAlbums
import us.huseli.thoucylinder.dataclasses.lastFm.getThumbnail
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.getSquareBitmapByUrl
import us.huseli.thoucylinder.getString
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class LastFmRepository @Inject constructor(
    database: Database,
    @ApplicationContext context: Context,
    playerRepo: PlayerRepository,
) : SharedPreferences.OnSharedPreferenceChangeListener, PlayerRepositoryListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val albumDao = database.albumDao()
    private val scrobbleMutex = Mutex()
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scrobbleJob: Job? = null

    private val _allTopAlbumsFetched = MutableStateFlow(false)
    private val _albumArtCache = MutexCache<String, ImageBitmap> { it.getSquareBitmapByUrl()?.asImageBitmap() }
    private val _topAlbums = MutableStateFlow<List<LastFmTopAlbumsResponse.Album>>(emptyList())
    private val _nextTopAlbumPage = MutableStateFlow(1)
    private val _username = MutableStateFlow(preferences.getString(PREF_LASTFM_USERNAME, null))
    private val _sessionKey = MutableStateFlow(preferences.getString(PREF_LASTFM_SESSION_KEY, null))
    private val _scrobbles = MutableStateFlow<List<LastFmScrobble>>(emptyList())
    private val _latestNowPlaying = MutableStateFlow<LastFmNowPlaying?>(null)
    private val _scrobble = MutableStateFlow(preferences.getBoolean(PREF_LASTFM_SCROBBLE, false))

    val allTopAlbumsFetched: StateFlow<Boolean> = _allTopAlbumsFetched.asStateFlow()
    val topAlbums: StateFlow<List<LastFmTopAlbumsResponse.Album>> = _topAlbums.asStateFlow()

    data class Session(
        val name: String,
        val key: String,
    )

    init {
        playerRepo.addListener(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

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

                Request.get(url).getLastFmTopAlbums()?.also {
                    _nextTopAlbumPage.value += 1
                    _topAlbums.value += it
                    if (it.isEmpty() || _nextTopAlbumPage.value >= LASTFM_PAGE_LIMIT) _allTopAlbumsFetched.value = true
                    return true
                }
            }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getSession(authToken: String): Session? {
        val xstream = XStream()
        val body = postAndGetString(method = "auth.getSession", params = mapOf("token" to authToken))

        return body?.let {
            xstream.alias("session", Session::class.java)
            xstream.alias("lfm", List::class.java)
            xstream.ignoreUnknownElements()
            xstream.addPermission(AnyTypePermission.ANY)
            (xstream.fromXML(body) as? List<Session>)?.firstOrNull()
        }
    }

    suspend fun getThumbnail(album: LastFmTopAlbumsResponse.Album): ImageBitmap? =
        album.image.getThumbnail()?.let { image -> _albumArtCache.get(image.url) }

    suspend fun listImportedAlbumIds(): List<String> = albumDao.listMusicBrainzReleaseIds()


    /** PRIVATE METHODS *******************************************************/
    private fun getApiUrl(params: Map<String, String> = emptyMap()): String {
        val allParams = params.plus("api_key" to BuildConfig.lastFmApiKey)
        val paramsString = allParams.toList().joinToString("&") { (key, value) -> "$key=$value" }

        return "$LASTFM_API_ROOT?$paramsString"
    }

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
                url = LASTFM_API_ROOT,
                formData = withSignature(
                    params.plus("method" to method).plus("api_key" to BuildConfig.lastFmApiKey)
                ),
            ).connect().getString()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "postAndGetString $method: $e", e)
            null
        }
    }

    private fun setUsername(value: String) {
        if (value != _username.value) {
            _username.value = value
            _topAlbums.value = emptyList()
            _allTopAlbumsFetched.value = false
            _nextTopAlbumPage.value = 1
        }
    }

    private fun withSignature(params: Map<String, String>): Map<String, String> =
        params.plus("api_sig" to getSignature(params))


    /** OVERRIDDEN METHODS ****************************************************/
    override suspend fun onPlaybackChange(combo: QueueTrackCombo?, state: PlayerRepository.PlaybackState) {
        _sessionKey.value?.also { sessionKey ->
            if (_scrobble.value) {
                val nowPlaying = combo?.let {
                    it.artist?.let { artist ->
                        LastFmNowPlaying(
                            artist = artist,
                            track = it.track.title,
                            duration = it.track.duration?.inWholeSeconds?.toInt(),
                            album = it.album?.title ?: it.spotifyAlbum?.name,
                            trackNumber = it.track.albumPosition,
                            albumArtist = it.album?.artist,
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
        val artist = combo.artist
        val duration = combo.track.duration

        if (artist != null && duration != null && duration > 30.seconds && _scrobble.value) {
            scrobbleMutex.withLock {
                _scrobbles.value += LastFmScrobble(
                    track = combo.track.title,
                    artist = artist,
                    album = combo.album?.title,
                    albumArtist = combo.album?.artist,
                    mbid = combo.track.musicBrainzId,
                    trackNumber = combo.track.albumPosition,
                    duration = combo.track.duration?.inWholeSeconds?.toInt(),
                    timestamp = startTimestamp,
                )
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_LASTFM_SESSION_KEY -> _sessionKey.value = preferences.getString(key, null)
            PREF_LASTFM_USERNAME -> preferences.getString(key, null)?.also { setUsername(it) }
            PREF_LASTFM_SCROBBLE -> _scrobble.value = preferences.getBoolean(key, false)
        }
    }
}
