package us.huseli.thoucylinder.repositories

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.toInstant
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_ACCESS_TOKEN
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES
import us.huseli.thoucylinder.Constants.SPOTIFY_REDIRECT_URL
import us.huseli.thoucylinder.Constants.SPOTIFY_USER_ALBUMS_URL
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.getSpotifyAlbums
import us.huseli.thoucylinder.dataclasses.combos.SpotifyAlbumCombo
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SpotifyRepository @Inject constructor(
    database: Database,
    @ApplicationContext context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val spotifyDao = database.spotifyDao()

    private val _accessToken = MutableStateFlow(preferences.getString(PREF_SPOTIFY_ACCESS_TOKEN, null))
    private val _accessTokenExpires = MutableStateFlow(
        preferences.getLong(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES, 0).takeIf { it > 0 }?.toInstant()
    )
    private val _allUserAlbumsFetched = MutableStateFlow(false)
    private val _nextUserAlbumIdx = MutableStateFlow(0)
    private val _requestInstants = MutableStateFlow<List<Instant>>(emptyList())
    private val _totalUserAlbumCount = MutableStateFlow<Int?>(null)
    private val _userAlbumCombos = MutableStateFlow<List<SpotifyAlbumCombo>>(emptyList())

    val allUserAlbumsFetched: StateFlow<Boolean> = _allUserAlbumsFetched.asStateFlow()
    val isAuthorized: Flow<Boolean?> = _accessTokenExpires.map { it?.let { it > Instant.now() } }
    val nextUserAlbumIdx: StateFlow<Int> = _nextUserAlbumIdx.asStateFlow()
    val requestCode = Random.nextInt(1, 10000)
    val totalUserAlbumCount: StateFlow<Int?> = _totalUserAlbumCount.asStateFlow()
    val userAlbumCombos: StateFlow<List<SpotifyAlbumCombo>> = _userAlbumCombos.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    private val lastMinuteRequestCount: Int
        get() {
            val oneMinuteAgo = Instant.now().minusSeconds(60)
            return _requestInstants.value.filter { it >= oneMinuteAgo }.size
        }

    fun authorize(activity: Activity) {
        if (!isAuthorized()) {
            val request = AuthorizationRequest.Builder(
                BuildConfig.spotifyClientId,
                AuthorizationResponse.Type.TOKEN,
                SPOTIFY_REDIRECT_URL,
            ).setScopes(arrayOf("user-library-read")).build()
            AuthorizationClient.openLoginActivity(activity, requestCode, request)
        }
    }

    suspend fun listImportedAlbumIds() = spotifyDao.listImportedAlbumIds()

    suspend fun fetchNextUserAlbums(): Boolean {
        if (!_allUserAlbumsFetched.value) {
            val url = "$SPOTIFY_USER_ALBUMS_URL?limit=50&offset=${_nextUserAlbumIdx.value}"

            apiRequest(url)?.getSpotifyAlbums()?.also { response ->
                _userAlbumCombos.value += response.items.map { it.toSpotifyAlbumCombo() }
                _nextUserAlbumIdx.value += response.items.size
                _allUserAlbumsFetched.value = response.next == null
                _totalUserAlbumCount.value = response.total
                return true
            }
        }
        return false
    }

    suspend fun getSpotifyAlbum(albumId: UUID): SpotifyAlbum? = spotifyDao.getSpotifyAlbum(albumId)

    suspend fun saveSpotifyAlbumCombo(combo: SpotifyAlbumCombo) = spotifyDao.upsertSpotifyAlbumCombo(combo)

    fun setAuthorizationResponse(value: AuthorizationResponse) {
        if (value.type == AuthorizationResponse.Type.TOKEN) {
            _accessToken.value = value.accessToken
            _accessTokenExpires.value = Instant.now().plusSeconds(value.expiresIn.toLong())
        } else {
            _accessToken.value = null
            _accessTokenExpires.value = null
            if (value.error != null) SnackbarEngine.addError(value.error)
        }
        preferences.edit()
            .putString(PREF_SPOTIFY_ACCESS_TOKEN, _accessToken.value)
            .putLong(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES, _accessTokenExpires.value?.epochSecond ?: 0)
            .apply()
    }

    private fun apiRequest(url: String): Request? {
        return if (lastMinuteRequestCount < REQUEST_LIMIT_PER_MINUTE) {
            _accessToken.value?.let { accessToken ->
                Request.get(url = url, headers = mapOf("Authorization" to "Bearer $accessToken"))
                    .also { _requestInstants.value += Instant.now() }
            }
        } else null
    }

    private fun isAuthorized() = _accessTokenExpires.value?.let { it > Instant.now() } == true

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_SPOTIFY_ACCESS_TOKEN -> _accessToken.value = preferences.getString(PREF_SPOTIFY_ACCESS_TOKEN, null)
            PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES -> _accessTokenExpires.value =
                preferences.getLong(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES, 0).takeIf { it > 0 }?.toInstant()
        }
    }

    companion object {
        // "Based on testing, we found that Spotify allows for approximately 180 requests per minute without returning
        // the error 429" -- https://community.spotify.com/t5/Spotify-for-Developers/Web-API-ratelimit/td-p/5330410
        // So let's be overly cautious.
        const val REQUEST_LIMIT_PER_MINUTE = 50
    }
}
