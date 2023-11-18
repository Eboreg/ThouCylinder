package us.huseli.thoucylinder.repositories

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.DiscogsMaster
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResults
import us.huseli.thoucylinder.getApiUserAgent
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

    suspend fun getMaster(masterId: Int): DiscogsMaster? =
        gson.fromJson(request("masters/$masterId"), DiscogsMaster::class.java)

    suspend fun searchMasters(query: String, artist: String? = null): DiscogsSearchResults? {
        val params = mutableMapOf("per_page" to "10", "query" to query, "type" to "master")
        if (artist != null) params["artist"] = artist
        return gson.fromJson(request("database/search", params), DiscogsSearchResults::class.java)
    }

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): String {
        val paramString = withContext(Dispatchers.IO) {
            params.plus("callback" to "data")
                .map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
                .joinToString("&")
        }
        val url = "https://api.discogs.com/${path.trimStart('/')}?$paramString"
        val headers = mapOf(
            "User-Agent" to getApiUserAgent(),
            "Authorization" to "Discogs key=$apiKey, secret=$apiSecret",
        )

        return Request(url, headers).getString().replace(Regex("^data\\((.*)\\)$"), "$1")
    }
}
