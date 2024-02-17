package us.huseli.thoucylinder.dataclasses

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.time.Instant

data class BaseOAuth2Token(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    val scope: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("refresh_token")
    val refreshToken: String,
)


data class OAuth2Token(
    val accessToken: String,
    val tokenType: String,
    val scope: String,
    val refreshToken: String,
    val expires: Instant,
) {
    fun expiresSoon() = expires.isBefore(Instant.now().plusSeconds(60L * 5))
    fun isExpired() = expires.isBefore(Instant.now())
    fun toJson(): String = gson.toJson(this)

    companion object {
        val gson: Gson = GsonBuilder().create()

        fun fromJson(value: String): OAuth2Token = gson.fromJson(value, OAuth2Token::class.java)

        fun fromBaseJson(value: String): OAuth2Token {
            val base = gson.fromJson(value, BaseOAuth2Token::class.java)

            return OAuth2Token(
                accessToken = base.accessToken,
                tokenType = base.tokenType,
                scope = base.scope,
                refreshToken = base.refreshToken,
                expires = Instant.now().plusSeconds(base.expiresIn.toLong()),
            )
        }
    }
}
