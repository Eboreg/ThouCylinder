package us.huseli.thoucylinder

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.OAuth2Token
import java.security.MessageDigest
import java.util.Base64

class SpotifyOAuth2(context: Context) {
    enum class AuthorizationStatus { AUTHORIZED, UNAUTHORIZED, UNKNOWN }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val token = MutableStateFlow(
        preferences.getString(Constants.PREF_SPOTIFY_OAUTH2_TOKEN, null)?.let { OAuth2Token.fromJson(it) }
    )

    val authorizationStatus = token.map { token ->
        if (token == null) AuthorizationStatus.UNAUTHORIZED
        else AuthorizationStatus.AUTHORIZED
    }

    fun getAuthUrl(): String {
        val codeVerifier = generateRandomString()
        val challenge = generateCodeChallenge(codeVerifier)
        val params = mapOf(
            "client_id" to BuildConfig.spotifyClientId,
            "response_type" to "code",
            "redirect_uri" to REDIRECT_URL,
            "scope" to "user-library-read",
            "code_challenge_method" to "S256",
            "code_challenge" to challenge,
        )

        preferences.edit().putString(Constants.PREF_SPOTIFY_CODE_VERIFIER, codeVerifier).apply()
        return Request.getUrl(AUTH_URL, params)
    }

    suspend fun getToken(): OAuth2Token? {
        return token.value?.let { token ->
            if (token.isExpired()) refreshToken(token)
            else {
                if (token.expiresSoon()) scope.launch { refreshToken(token) }
                token
            }
        }
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

    private suspend fun fetchToken(code: String): OAuth2Token {
        val codeVerifier = preferences.getString(Constants.PREF_SPOTIFY_CODE_VERIFIER, null)
            ?: throw Exception("No codeVerifier found in preferences")
        val response = Request.postFormData(
            url = TOKEN_URL,
            formData = mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to REDIRECT_URL,
                "client_id" to BuildConfig.spotifyClientId,
                "code_verifier" to codeVerifier,
            ),
        )
        return saveToken(response.getString())
    }

    private suspend fun refreshToken(token: OAuth2Token): OAuth2Token {
        val response = Request.postFormData(
            url = TOKEN_URL,
            formData = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to token.refreshToken,
                "client_id" to BuildConfig.spotifyClientId,
            )
        )
        return saveToken(response.getString())
    }

    private fun saveToken(json: String): OAuth2Token {
        return OAuth2Token.fromBaseJson(json).also {
            token.value = it
            preferences.edit().putString(Constants.PREF_SPOTIFY_OAUTH2_TOKEN, it.toJson()).apply()
        }
    }

    companion object {
        const val AUTH_URL = "https://accounts.spotify.com/authorize"
        const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        const val REDIRECT_URL = "klaatu://${BuildConfig.hostName}/spotify/import-albums"
    }
}
