package us.huseli.thoucylinder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_CODE_VERIFIER
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_OAUTH2_TOKEN_CC
import us.huseli.thoucylinder.Constants.PREF_SPOTIFY_OAUTH2_TOKEN_PKCE
import us.huseli.thoucylinder.dataclasses.OAuth2Token
import us.huseli.thoucylinder.dataclasses.RefreshableOAuth2Token
import java.security.MessageDigest
import java.util.Base64

enum class AuthorizationStatus { AUTHORIZED, UNAUTHORIZED, UNKNOWN }

abstract class AbstractSpotifyOAuth2<T : OAuth2Token>(private val tokenPrefKey: String, context: Context) : ILogger {
    protected val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val token: MutableStateFlow<T?> = MutableStateFlow(
        preferences.getString(tokenPrefKey, null)?.let { jsonToToken(it) }
    )

    val authorizationStatus: Flow<AuthorizationStatus> = token.map { token ->
        if (token == null) AuthorizationStatus.UNAUTHORIZED
        else AuthorizationStatus.AUTHORIZED
    }

    abstract fun baseJsonToToken(json: String): T
    abstract suspend fun getAccessToken(): String?
    abstract fun jsonToToken(json: String): T?

    fun clearToken() {
        token.value = null
        preferences.edit().remove(tokenPrefKey).apply()
    }

    protected fun saveToken(json: String): T = baseJsonToToken(json).also {
        token.value = it
        preferences.edit().putString(tokenPrefKey, it.toJson()).apply()
    }

    companion object {
        const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    }
}


class SpotifyOAuth2ClientCredentials(context: Context) :
    AbstractSpotifyOAuth2<OAuth2Token>(PREF_SPOTIFY_OAUTH2_TOKEN_CC, context) {
    override fun baseJsonToToken(json: String): OAuth2Token = OAuth2Token.fromBaseJson(json)

    override suspend fun getAccessToken(): String? = token.value.let { token ->
        if (token == null || token.isExpired()) fetchToken()?.accessToken
        else token.accessToken
    }

    override fun jsonToToken(json: String): OAuth2Token? = OAuth2Token.fromJson(json)

    private suspend fun fetchToken(): OAuth2Token? = try {
        val auth = Base64.getEncoder()
            .encode("${BuildConfig.spotifyClientId}:${BuildConfig.spotifyClientSecret}".toByteArray(Charsets.UTF_8))
            .toString(Charsets.UTF_8)
        val response = Request.postFormData(
            url = TOKEN_URL,
            formData = mapOf("grant_type" to "client_credentials"),
            headers = mapOf("Authorization" to "Basic $auth"),
        )

        saveToken(response.getString())
    } catch (e: HTTPResponseError) {
        logError(e)
        token.value = null
        null
    }
}


class SpotifyOAuth2PKCE(context: Context) :
    AbstractSpotifyOAuth2<RefreshableOAuth2Token>(PREF_SPOTIFY_OAUTH2_TOKEN_PKCE, context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getAuthUrl(): String {
        val codeVerifier = generateRandomString()
        val challenge = generateCodeChallenge(codeVerifier)
        val params = mapOf(
            "client_id" to BuildConfig.spotifyClientId,
            "response_type" to "code",
            "redirect_uri" to IMPORT_ALBUMS_REDIRECT_URL,
            "scope" to "user-library-read",
            "code_challenge_method" to "S256",
            "code_challenge" to challenge,
        )

        preferences.edit().putString(PREF_SPOTIFY_CODE_VERIFIER, codeVerifier).apply()
        return Request.getUrl(AUTH_URL, params)
    }

    suspend fun handleIntent(intent: Intent) {
        intent.data?.pathSegments?.also { pathSegments ->
            if (pathSegments.getOrNull(0) == "spotify") {
                val code = intent.data?.getQueryParameter("code")
                val error = intent.data?.getQueryParameter("error")

                if (code != null) fetchToken(code)
                else if (error != null) throw Exception(error)
            }
        }
    }

    override fun baseJsonToToken(json: String): RefreshableOAuth2Token = RefreshableOAuth2Token.fromBaseJson(json)

    override suspend fun getAccessToken(): String? = token.value?.let { token ->
        if (token.isExpired()) refreshToken(token)?.accessToken
        else {
            if (token.expiresSoon()) scope.launch { refreshToken(token) }
            token.accessToken
        }
    }

    override fun jsonToToken(json: String): RefreshableOAuth2Token? = RefreshableOAuth2Token.fromJson(json)

    private suspend fun fetchToken(code: String): OAuth2Token? = try {
        val codeVerifier = preferences.getString(PREF_SPOTIFY_CODE_VERIFIER, null)
            ?: throw Exception("No codeVerifier found in preferences")
        val response = Request.postFormData(
            url = TOKEN_URL,
            formData = mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to IMPORT_ALBUMS_REDIRECT_URL,
                "client_id" to BuildConfig.spotifyClientId,
                "code_verifier" to codeVerifier,
            ),
        )

        saveToken(response.getString())
    } catch (e: HTTPResponseError) {
        logError(e)
        token.value = null
        null
    }

    private fun generateCodeChallenge(randomString: String): String {
        val hashed = MessageDigest.getInstance("SHA-256").digest(randomString.toByteArray(Charsets.UTF_8))
        val codeChallenge = Base64.getEncoder().encode(hashed)

        return codeChallenge.toString(Charsets.UTF_8)
            .replace("=", "")
            .replace("+", "-")
            .replace("/", "_")
    }

    private fun generateRandomString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return List(128) { charPool.random() }.joinToString("")
    }

    private suspend fun refreshToken(token: RefreshableOAuth2Token): RefreshableOAuth2Token? = try {
        val response = Request.postFormData(
            url = TOKEN_URL,
            formData = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to token.refreshToken,
                "client_id" to BuildConfig.spotifyClientId,
            )
        )

        saveToken(response.getString())
    } catch (e: HTTPResponseError) {
        logError(e)
        this.token.value = null
        null
    }

    companion object {
        const val AUTH_URL = "https://accounts.spotify.com/authorize"
        const val IMPORT_ALBUMS_REDIRECT_URL = "klaatu://${BuildConfig.hostName}/spotify/import-albums"
    }
}
