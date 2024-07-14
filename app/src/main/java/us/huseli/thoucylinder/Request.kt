package us.huseli.thoucylinder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.dataclasses.HTTPContentRange
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.interfaces.ILogger
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt
import kotlin.text.Charsets.UTF_8

class HTTPResponseError(val url: String, method: Request.Method, val code: Int?, message: String?) :
    Exception("HTTP $code: ${message ?: "No message"} ($method $url)")

data class Request(
    val url: String,
    private val headers: Map<String, String> = emptyMap(),
    val method: Method = Method.GET,
    private val body: String? = null,
    private val connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
    private val readTimeout: Int = DEFAULT_READ_TIMEOUT,
) : ILogger {
    constructor(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        method: Method = Method.GET,
        body: String? = null,
        connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
        readTimeout: Int = DEFAULT_READ_TIMEOUT,
    ) : this(
        url = getUrl(url, params),
        headers = headers,
        method = method,
        body = body,
        connectTimeout = connectTimeout,
        readTimeout = readTimeout,
    )

    enum class Method(val value: String) { GET("GET"), POST("POST") }

    private var requestStart: Long? = null

    var contentRange: HTTPContentRange? = null
        private set
    var contentLength: Int? = null
        private set
    var responseCode: Int? = null
        private set

    private suspend fun connect(): HttpURLConnection = withContext(Dispatchers.IO) {
        requestStart = System.currentTimeMillis()
        log(Log.DEBUG, "Request", "START ${method.value} $url")
        if (body != null) log(Log.DEBUG, "Request", "BODY $body")

        (URL(url).openConnection() as HttpURLConnection).also { conn ->
            if (BuildConfig.DEBUG) {
                conn.connectTimeout = 0
                conn.readTimeout = 0
            } else {
                conn.connectTimeout = connectTimeout
                conn.readTimeout = readTimeout
            }
            conn.requestMethod = method.value
            headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
            if (body != null && method == Method.POST) {
                val binaryBody = body.toByteArray(UTF_8)
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(binaryBody.size)
                conn.outputStream.write(binaryBody, 0, binaryBody.size)
            }
            val responseCode = try {
                conn.responseCode
            } catch (e: Throwable) {
                throw HTTPResponseError(this@Request.url, method, null, e.message)
            }
            this@Request.responseCode = responseCode
            if (responseCode >= 400) throw HTTPResponseError(
                url = this@Request.url,
                method = method,
                code = responseCode,
                message = conn.responseMessage,
            )
            contentRange = conn.getHeaderField("Content-Range")?.parseContentRange()
            contentLength = contentRange?.size ?: conn.getHeaderField("Content-Length")?.toInt()
        }
    }

    fun finish(receivedBytes: Int? = null) {
        requestStart?.also { start ->
            val elapsed = (System.currentTimeMillis() - start).toDouble() / 1000
            var message = "FINISH ${method.value} $url: ${elapsed}s"
            if (receivedBytes != null) {
                val kbps = ((receivedBytes / elapsed) / 1024).roundToInt()
                message += ", ${receivedBytes}B ($kbps KB/s)"
            }

            log("Request", message)
        }
    }

    suspend fun getBitmap(): Bitmap? = getInputStream().use { BitmapFactory.decodeStream(it) }.also { finish() }

    suspend fun getJson(): Map<String, *> = getInputStream().use {
        gson.fromJson(it.bufferedReader(), jsonResponseType) ?: emptyMap<String, Any>()
    }.also { finish() }

    suspend inline fun <reified T> getObject(): T =
        getInputStream().use { gson.fromJson(it.bufferedReader(), T::class.java) }.also { finish() }

    suspend inline fun <reified T> getObjectOrNull(): T? {
        return try {
            getInputStream().use { gson.fromJson(it.bufferedReader(), T::class.java) }.also { finish() }
        } catch (e: Exception) {
            logError("getObjectOrNull(): $method $url", e)
            null
        }
    }

    suspend fun getString(): String =
        getInputStream().use { it.bufferedReader().readText() }.also { finish(it.length) }

    suspend fun getInputStream(): InputStream = withContext(Dispatchers.IO) {
        val conn = connect()
        val isGzipped = conn.headerFields["Content-Encoding"]?.contains("gzip") ?: false

        if (isGzipped) GZIPInputStream(conn.inputStream) else conn.inputStream
    }

    companion object {
        const val DEFAULT_READ_TIMEOUT = 10_000
        const val DEFAULT_CONNECT_TIMEOUT = 4_050

        val gson: Gson = GsonBuilder().create()
        val jsonResponseType = object : TypeToken<Map<String, *>>() {}

        fun getUrl(url: String, params: Map<String, String> = emptyMap()) =
            if (params.isNotEmpty()) encodeQuery(params).let { if (url.contains("?")) "$url&$it" else "$url?$it" }
            else url

        fun postJson(
            url: String,
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            json: Map<String, *>,
        ) =
            Request(
                url = url,
                params = params,
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

abstract class DeferredRequestJob(val url: String, val lowPrio: Boolean = false) {
    private val onErrorListeners = mutableListOf<(Throwable) -> Unit>()
    private val onFinishedListeners = mutableListOf<(String?) -> Unit>()

    val created = System.currentTimeMillis()
    val lock = Mutex(true)
    var isStarted = false
        private set

    abstract suspend fun request(): String?

    fun addOnErrorListener(value: (Throwable) -> Unit) {
        onErrorListeners.add(value)
    }

    fun addOnFinishedListener(value: (String?) -> Unit) {
        onFinishedListeners.add(value)
    }

    suspend fun run(): String? {
        /**
         * This method will suspend until `lock` is unlocked, which needs to be done by some outside stateholder when
         * it determines it's this job's time to run.
         */
        return lock.withLock {
            isStarted = true
            withContext(Dispatchers.IO) {
                try {
                    request().also { response ->
                        onFinishedListeners.forEach { it.invoke(response) }
                    }
                } catch (e: Throwable) {
                    onErrorListeners.forEach { it.invoke(e) }
                    null
                }
            }
        }
    }

    override fun equals(other: Any?) = other is DeferredRequestJob && other.url == url && other.created == created
    override fun toString() = "<${javaClass.simpleName} $url>"
    override fun hashCode(): Int = 31 * url.hashCode() + created.hashCode()
}

abstract class DeferredRequestJobManager<T : DeferredRequestJob>(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ILogger {
    private val queue = MutableStateFlow<List<T>>(emptyList())
    private val nextJob = queue.map { it.getNext() }.filterNotNull().distinctUntilChanged()

    init {
        scope.launch {
            nextJob.collect { job ->
                waitBeforeUnlocking()
                if (job.lock.isLocked) job.lock.unlock()
            }
        }
    }

    open fun onJobError(job: T, error: Throwable) {
        logError(error)
    }

    abstract fun onJobFinished(job: T, response: String?, timestamp: Long)
    abstract suspend fun waitBeforeUnlocking()

    suspend fun runJob(job: T): String? {
        job.addOnFinishedListener { queue.value -= job }
        job.addOnFinishedListener { onJobFinished(job, it, System.currentTimeMillis()) }
        job.addOnErrorListener { onJobError(job, it) }
        queue.value += job
        return job.run()
    }
}

fun <T : DeferredRequestJob> Collection<T>.getNext(): T? =
    filter { !it.isStarted && !it.lowPrio && it.lock.isLocked }.minByOrNull { it.created }
        ?: filter { !it.isStarted && it.lock.isLocked }.minByOrNull { it.created }
