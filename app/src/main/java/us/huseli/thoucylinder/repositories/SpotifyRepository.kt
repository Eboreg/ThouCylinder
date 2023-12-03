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
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_ACCESS_TOKEN
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.SpotifyResponse
import us.huseli.thoucylinder.dataclasses.SpotifyResponseAlbumItem
import us.huseli.thoucylinder.dataclasses.getSpotifyAlbums
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyAlbumPojo
import us.huseli.thoucylinder.toInstant
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class SpotifyRepository @Inject constructor(
    database: Database,
    @ApplicationContext context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val requestCode = Random.nextInt(1, 10000)
    private val redirectUri = "klaatu://thoucylinder/spotify/import-albums"
    private val spotifyDao = database.spotifyDao()
    private val _accessToken = MutableStateFlow(preferences.getString(PREF_SPOTIFY_ACCESS_TOKEN, null))
    private val _accessTokenExpires = MutableStateFlow(
        preferences.getLong(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES, 0).takeIf { it > 0 }?.toInstant()
    )

    val isAuthorized: Flow<Boolean> = _accessTokenExpires.map { it?.let { it > Instant.now() } == true }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun authorize(activity: Activity) {
        if (!isAuthorized()) {
            val request = AuthorizationRequest.Builder(
                BuildConfig.spotifyClientId,
                AuthorizationResponse.Type.TOKEN,
                redirectUri,
            ).setScopes(arrayOf("user-library-read")).build()
            AuthorizationClient.openLoginActivity(activity, requestCode, request)
        }
    }

    suspend fun fetchUserAlbums(offset: Int): SpotifyResponse<SpotifyResponseAlbumItem>? {
        return _accessToken.value?.let { accessToken ->
            Request(
                urlString = "https://api.spotify.com/v1/me/albums?limit=50&offset=$offset",
                headers = mapOf("Authorization" to "Bearer $accessToken"),
                method = Request.Method.GET,
            ).getSpotifyAlbums()
        }
    }

    suspend fun listImportedAlbumIds() = spotifyDao.listImportedAlbumIds()

    suspend fun saveSpotifyAlbumPojo(pojo: SpotifyAlbumPojo) = spotifyDao.upsertSpotifyAlbumPojo(pojo)

    fun setAuthorizationResponse(value: AuthorizationResponse) {
        if (value.type == AuthorizationResponse.Type.TOKEN) {
            _accessToken.value = value.accessToken
            _accessTokenExpires.value = Instant.now().plusSeconds(value.expiresIn.toLong())
        } else {
            _accessToken.value = null
            _accessTokenExpires.value = null
        }
        preferences.edit()
            .putString(PREF_SPOTIFY_ACCESS_TOKEN, _accessToken.value)
            .putLong(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES, _accessTokenExpires.value?.epochSecond ?: 0)
            .apply()
    }

    private fun isAuthorized() = _accessTokenExpires.value?.let { it > Instant.now() } == true

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_SPOTIFY_ACCESS_TOKEN -> _accessToken.value = preferences.getString(PREF_SPOTIFY_ACCESS_TOKEN, null)
            PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES -> _accessTokenExpires.value =
                preferences.getLong(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES, 0).takeIf { it > 0 }?.toInstant()
        }
    }
}
