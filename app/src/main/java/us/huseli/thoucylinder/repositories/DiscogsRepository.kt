package us.huseli.thoucylinder.repositories

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.dataclasses.DiscogsMaster
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResults
import us.huseli.thoucylinder.urlRequest
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscogsRepository @Inject constructor() {
    /**
     * Possible DiscogsMasterTrack.position formats:
     *  - "<discno>-<trackno>" (e.g. "2-12")
     *  - "<LP side><trackno>" (e.g. "B4")
     *  - "<trackno>" (e.g. "12")
     */
    private val apiKey = BuildConfig.discogsApiKey
    private val apiSecret = BuildConfig.discogsApiSecret
    private val gson: Gson = GsonBuilder().create()

    suspend fun getMaster(masterId: Int): DiscogsMaster? {
        return gson.fromJson(request("masters/$masterId"), DiscogsMaster::class.java).also {
            Log.i("DiscogsRepository", "getMaster: $it")
        }
    }

    suspend fun searchMasters(query: String, artist: String? = null): DiscogsSearchResults? {
        val params = mutableMapOf("per_page" to "10", "query" to query, "type" to "master")
        if (artist != null) params["artist"] = artist
        return gson.fromJson(request("database/search", params), DiscogsSearchResults::class.java).also {
            Log.i("DiscogsRepository", "searchMasters: $it")
        }
    }

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): String {
        val paramString = withContext(Dispatchers.IO) {
            params.plus("callback" to "data")
                .map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
                .joinToString("&")
        }
        val url = "https://api.discogs.com/${path.trimStart('/')}?$paramString"
        val conn = urlRequest(
            urlString = url,
            headers = mapOf(
                "User-Agent" to "ThouCylinder/${BuildConfig.VERSION_NAME}",
                "Authorization" to "Discogs key=$apiKey, secret=$apiSecret",
            ),
        )
        return withContext(Dispatchers.IO) {
            conn.getInputStream().use { it.bufferedReader().readText() }
                .replace(Regex("^data\\((.*)\\)$"), "$1")
                .also { Log.i("DiscogsRepository", "request: url=$url, body=$it") }
        }
    }
}
