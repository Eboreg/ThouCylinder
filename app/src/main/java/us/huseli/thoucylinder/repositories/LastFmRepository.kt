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
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SCROBBLE
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SESSION_KEY
import us.huseli.thoucylinder.Constants.PREF_LASTFM_USERNAME
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmNowPlaying
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmScrobble
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.getFullImage
import us.huseli.thoucylinder.dataclasses.lastFm.getLastFmAlbum
import us.huseli.thoucylinder.dataclasses.lastFm.getLastFmTopAlbums
import us.huseli.thoucylinder.dataclasses.lastFm.getThumbnail
import us.huseli.thoucylinder.dataclasses.pojos.LastFmAlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.getBitmapByUrl
import us.huseli.thoucylinder.getString
import us.huseli.thoucylinder.join
import us.huseli.thoucylinder.md5
import us.huseli.thoucylinder.toHex
import java.net.URLConnection
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
    private val lastFmDao = database.lastFmDao()
    private val scrobbleMutex = Mutex()
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scrobbleJob: Job? = null

    private val _allTopAlbumsFetched = MutableStateFlow(false)
    private val _albumArtCache = mutableMapOf<String, ImageBitmap>()
    private val _topAlbums = MutableStateFlow<List<LastFmTopAlbumsResponse.TopAlbums.Album>>(emptyList())
    private val _nextTopAlbumPage = MutableStateFlow(1)
    private val _username = MutableStateFlow(preferences.getString(PREF_LASTFM_USERNAME, null))
    private val _sessionKey = MutableStateFlow(preferences.getString(PREF_LASTFM_SESSION_KEY, null))
    private val _scrobbles = MutableStateFlow<List<LastFmScrobble>>(emptyList())
    private val _latestNowPlaying = MutableStateFlow<LastFmNowPlaying?>(null)
    private val _scrobble = MutableStateFlow(preferences.getBoolean(PREF_LASTFM_SCROBBLE, false))

    val allTopAlbumsFetched: StateFlow<Boolean> = _allTopAlbumsFetched.asStateFlow()
    val topAlbums: StateFlow<List<LastFmTopAlbumsResponse.TopAlbums.Album>> = _topAlbums.asStateFlow()

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
                                    post(
                                        method = "track.scrobble",
                                        params = chunk
                                            .mapIndexed { index, scrobble -> scrobble.toMap(index) }
                                            .join()
                                            .plus("sk" to sessionKey),
                                    )?.also {
                                        scrobbleMutex.withLock { _scrobbles.value -= chunk }
                                        Log.i(javaClass.simpleName, it.getString())
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
                    if (it.isEmpty() || _nextTopAlbumPage.value >= PAGE_LIMIT) _allTopAlbumsFetched.value = true
                    return true
                }
            }
        }
        return false
    }

    suspend fun getFullImage(album: LastFmTopAlbumsResponse.TopAlbums.Album): ImageBitmap? {
        return album.image.getFullImage()?.let { image ->
            if (_albumArtCache.contains(image.url)) _albumArtCache[image.url]
            else image.url.getBitmapByUrl()?.asImageBitmap()?.also { _albumArtCache[image.url] = it }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getSession(authToken: String): Session? {
        val xstream = XStream()
        val body = post(
            method = "auth.getSession",
            params = mapOf("token" to authToken),
            failSilently = false,
        )?.getString()

        return body?.let {
            xstream.alias("session", Session::class.java)
            xstream.alias("lfm", List::class.java)
            xstream.ignoreUnknownElements()
            xstream.addPermission(AnyTypePermission.ANY)
            (xstream.fromXML(body) as? List<Session>)?.firstOrNull()
        }
    }

    suspend fun getThumbnail(album: LastFmTopAlbumsResponse.TopAlbums.Album): ImageBitmap? {
        return album.image.getThumbnail()?.let { image ->
            if (_albumArtCache.contains(image.url)) _albumArtCache[image.url]
            else image.url.getBitmapByUrl()?.asImageBitmap()?.also { _albumArtCache[image.url] = it }
        }
    }

    suspend fun insertLastFmAlbumPojo(pojo: LastFmAlbumPojo) = lastFmDao.insertLastFmAlbumPojo(pojo)

    suspend fun listImportedAlbumIds(): List<String> = lastFmDao.listImportedAlbumIds()

    suspend fun topAlbumToAlbum(topAlbum: LastFmTopAlbumsResponse.TopAlbums.Album): LastFmAlbum? {
        val url = getApiUrl(mapOf("method" to "album.getinfo", "format" to "json", "mbid" to topAlbum.mbid))
        return Request.get(url).getLastFmAlbum()?.toEntity(topAlbum.mbid, topAlbum.artist)
    }


    /** PRIVATE METHODS *******************************************************/
    private fun getApiUrl(params: Map<String, String> = emptyMap()): String {
        val allParams = params.plus("api_key" to BuildConfig.lastFmApiKey)
        val paramsString = allParams.toList().joinToString("&") { (key, value) -> "$key=$value" }

        return "$API_ROOT?$paramsString"
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

    private suspend fun post(method: String, params: Map<String, String>, failSilently: Boolean = true): URLConnection? {
        return try {
            Request.postFormData(
                url = API_ROOT,
                formData = withSignature(
                    params.plus("method" to method).plus("api_key" to BuildConfig.lastFmApiKey)
                ),
            ).connect()
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "post $method: $e", e)
            if (!failSilently) throw e
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
    override suspend fun onPlaybackChange(pojo: QueueTrackPojo?, state: PlayerRepository.PlaybackState) {
        _sessionKey.value?.also { sessionKey ->
            if (_scrobble.value) {
                val nowPlaying = pojo?.let {
                    it.artist?.let { artist ->
                        LastFmNowPlaying(
                            artist = artist,
                            track = it.track.title,
                            duration = it.track.duration?.inWholeSeconds?.toInt(),
                            album = it.album?.title ?: it.spotifyAlbum?.name,
                            trackNumber = it.track.albumPosition,
                            albumArtist = it.album?.artist,
                            mbid = it.lastFmTrack?.musicBrainzId,
                        )
                    }
                }

                if (nowPlaying != null && nowPlaying != _latestNowPlaying.value) {
                    post(
                        method = "track.updateNowPlaying",
                        params = nowPlaying.toMap().plus("sk" to sessionKey)
                    )?.also { Log.i(javaClass.simpleName, it.getString()) }
                }
                _latestNowPlaying.value = nowPlaying
            }
        }
    }

    override suspend fun onHalfTrackPlayed(pojo: QueueTrackPojo, startTimestamp: Long) {
        val artist = pojo.artist
        val duration = pojo.track.duration

        if (artist != null && duration != null && duration > 30.seconds && _scrobble.value) {
            scrobbleMutex.withLock {
                _scrobbles.value += LastFmScrobble(
                    track = pojo.track.title,
                    artist = artist,
                    album = pojo.album?.title,
                    albumArtist = pojo.album?.artist,
                    mbid = pojo.lastFmTrack?.musicBrainzId,
                    trackNumber = pojo.track.albumPosition,
                    duration = pojo.track.duration?.inWholeSeconds?.toInt(),
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

    companion object {
        // Don't know if Last.fm has any hard quota limit, but better not
        // overdo it.
        const val PAGE_LIMIT = 20
        const val API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    }
}
