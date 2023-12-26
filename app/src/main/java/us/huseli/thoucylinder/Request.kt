package us.huseli.thoucylinder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.Constants.URL_CONNECT_TIMEOUT
import us.huseli.thoucylinder.Constants.URL_READ_TIMEOUT
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

class Request(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val method: Method = Method.GET,
    private val body: ByteArray? = null,
) {
    enum class Method(val value: String) { GET("GET"), POST("POST") }

    private val urlObject = URL(url)

    suspend fun getString(): String =
        withContext(Dispatchers.IO) { getInputStream().use { it.bufferedReader().readText() } }

    suspend fun connect(): URLConnection = withContext(Dispatchers.IO) {
        Log.i("Request", "${method.value} $url")
        urlObject.openConnection().apply {
            if (BuildConfig.DEBUG) {
                connectTimeout = 0
                readTimeout = 0
            } else {
                connectTimeout = URL_CONNECT_TIMEOUT
                readTimeout = URL_READ_TIMEOUT
            }
            (this as? HttpURLConnection)?.requestMethod = method.value
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null && method == Method.POST) {
                doOutput = true
                (this as? HttpURLConnection)?.setFixedLengthStreamingMode(body.size)
                getOutputStream().write(body, 0, body.size)
            }
        }
    }

    private suspend fun getInputStream(): InputStream =
        withContext(Dispatchers.IO) { connect().run { getInputStream() } }

    companion object {
        val gson: Gson = GsonBuilder().create()
        val jsonResponseType = object : TypeToken<Map<String, *>>() {}

        fun get(url: String, headers: Map<String, String> = emptyMap()) =
            Request(url = url, headers = headers, method = Method.GET)

        fun postJson(url: String, headers: Map<String, String> = emptyMap(), json: Map<String, *>) =
            Request(
                url = url,
                headers = headers.plus("Content-Type" to "application/json"),
                body = gson.toJson(json).toByteArray(UTF_8),
                method = Method.POST,
            )

        fun postFormData(url: String, headers: Map<String, String> = emptyMap(), formData: Map<String, String>) =
            Request(
                url = url,
                headers = headers.plus("Content-Type" to "application/x-www-form-urlencoded"),
                body = formData
                    .map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
                    .joinToString("&")
                    .toByteArray(UTF_8),
                method = Method.POST,
            )
    }
}

suspend fun URLConnection.getString(): String =
    withContext(Dispatchers.IO) { getInputStream().use { it.bufferedReader().readText() } }

suspend fun <T> URLConnection.getObject(responseType: TypeToken<T>): T? =
    getString().let { Request.gson.fromJson(it, responseType) }

suspend fun URLConnection.getJson(): Map<String, *> = getString().let {
    Request.gson.fromJson(it, Request.jsonResponseType) ?: emptyMap<String, Any>()
}

suspend fun URLConnection.getBitmap(): Bitmap? =
    withContext(Dispatchers.IO) { getInputStream().use { BitmapFactory.decodeStream(it) } }
