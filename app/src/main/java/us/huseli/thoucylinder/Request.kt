package us.huseli.thoucylinder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

data class Request(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val method: Method = Method.GET,
    private val body: String? = null,
) {
    constructor(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        method: Method = Method.GET,
        body: String? = null,
    ) : this(url = getUrl(url, params), headers = headers, method = method, body = body)

    enum class Method(val value: String) { GET("GET"), POST("POST") }

    suspend fun connect(): URLConnection = withContext(Dispatchers.IO) {
        Log.i("Request", "${method.value} $url")
        URL(url).openConnection().apply {
            if (BuildConfig.DEBUG) {
                connectTimeout = 0
                readTimeout = 0
            } else {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
            }
            (this as? HttpURLConnection)?.requestMethod = method.value
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null && method == Method.POST) {
                val binaryBody = body.toByteArray(UTF_8)
                doOutput = true
                (this as? HttpURLConnection)?.setFixedLengthStreamingMode(binaryBody.size)
                getOutputStream().write(binaryBody, 0, binaryBody.size)
            }
        }
    }

    suspend fun getBitmap(): Bitmap? =
        withContext(Dispatchers.IO) { getInputStream().use { BitmapFactory.decodeStream(it) } }

    suspend fun getJson(): Map<String, *> = withContext(Dispatchers.IO) {
        getString().let { gson.fromJson(it, jsonResponseType) ?: emptyMap<String, Any>() }
    }

    suspend inline fun <reified T> getObject(): T? =
        withContext(Dispatchers.IO) { gson.fromJson(getString(), T::class.java) }

    suspend fun getString(): String = try {
        withContext(Dispatchers.IO) { getInputStream().use { it.bufferedReader().readText() } }
    } catch (e: Exception) {
        Log.e(javaClass.simpleName, "getString [url=$url]: $e", e)
        throw e
    }

    private suspend fun getInputStream(): InputStream =
        withContext(Dispatchers.IO) { connect().run { getInputStream() } }

    companion object {
        const val READ_TIMEOUT = 10_000
        const val CONNECT_TIMEOUT = 4_050

        val gson: Gson = GsonBuilder().create()
        val jsonResponseType = object : TypeToken<Map<String, *>>() {}

        fun getUrl(url: String, params: Map<String, String> = emptyMap()) =
            if (params.isNotEmpty()) encodeQuery(params).let { if (url.contains("?")) "$url&$it" else "$url?$it" } else url

        fun postJson(url: String, headers: Map<String, String> = emptyMap(), json: Map<String, *>) =
            Request(
                url = url,
                headers = headers.plus("Content-Type" to "application/json"),
                body = gson.toJson(json),
                method = Method.POST,
            )

        fun postFormData(url: String, headers: Map<String, String> = emptyMap(), formData: Map<String, String>) =
            Request(
                url = url,
                headers = headers.plus("Content-Type" to "application/x-www-form-urlencoded"),
                body = encodeQuery(formData),
                method = Method.POST,
            )

        private fun encodeQuery(params: Map<String, String>) =
            params.map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }.joinToString("&")
    }
}
