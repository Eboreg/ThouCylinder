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

class Request(
    private val urlString: String,
    private val headers: Map<String, String> = emptyMap(),
    private val method: Method = Method.GET,
    private val body: ByteArray? = null,
) {
    enum class Method(val value: String) { GET("GET"), POST("POST") }

    private val url = URL(urlString)
    private val jsonResponseType = object : TypeToken<Map<String, *>>() {}

    suspend fun getBitmap(): Bitmap? =
        withContext(Dispatchers.IO) { getInputStream().use { BitmapFactory.decodeStream(it) } }

    suspend fun getJson(): Map<String, *> = getString().let {
        val gson: Gson = GsonBuilder().create()
        gson.fromJson(it, jsonResponseType) ?: emptyMap<String, Any>()
    }

    suspend fun getString(): String =
        withContext(Dispatchers.IO) { getInputStream().use { it.bufferedReader().readText() } }

    suspend fun openConnection(): URLConnection = withContext(Dispatchers.IO) {
        Log.i("Request", "${method.value} $urlString")
        url.openConnection().apply {
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
                getOutputStream().write(body, 0, body.size)
            }
        }
    }

    private suspend fun getInputStream(): InputStream =
        withContext(Dispatchers.IO) { openConnection().run { getInputStream() } }
}
