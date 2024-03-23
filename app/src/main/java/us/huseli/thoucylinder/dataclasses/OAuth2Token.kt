package us.huseli.thoucylinder.dataclasses

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

data class BaseOAuth2Token(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
)


data class BaseRefreshableOAuth2Token(
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


open class OAuth2Token(val accessToken: String, val expires: Long) {
    fun expiresSoon() = expires < System.currentTimeMillis().plus(60 * 5)
    fun isExpired() = expires < System.currentTimeMillis()
    fun toJson(): String = gson.toJson(this)

    companion object {
        val gson: Gson = GsonBuilder().create()

        fun fromBaseJson(value: String): OAuth2Token {
            val base = gson.fromJson(value, BaseOAuth2Token::class.java)

            return OAuth2Token(
                accessToken = base.accessToken,
                expires = System.currentTimeMillis().plus(base.expiresIn * 1000),
            )
        }

        fun fromJson(value: String): OAuth2Token? {
            return try {
                gson.fromJson(value, OAuth2Token::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}


class RefreshableOAuth2Token(
    accessToken: String,
    val scope: String,
    val refreshToken: String,
    expires: Long,
) : OAuth2Token(accessToken, expires) {
    companion object {
        fun fromBaseJson(value: String): RefreshableOAuth2Token {
            val base = gson.fromJson(value, BaseRefreshableOAuth2Token::class.java)

            return RefreshableOAuth2Token(
                accessToken = base.accessToken,
                scope = base.scope,
                refreshToken = base.refreshToken,
                expires = System.currentTimeMillis().plus(base.expiresIn * 1000),
            )
        }

        fun fromJson(value: String): RefreshableOAuth2Token? {
            return try {
                gson.fromJson(value, RefreshableOAuth2Token::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
