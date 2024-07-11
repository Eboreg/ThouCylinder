package us.huseli.thoucylinder.repositories

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.DiscogsMaster
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResults
import us.huseli.thoucylinder.getMutexCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscogsRepository @Inject constructor() : AbstractScopeHolder() {
    /**
     * Possible DiscogsMasterTrack.position formats:
     *  - "<discno>-<trackno>" (e.g. "2-12")
     *  - "<LP side><trackno>" (e.g. "B4")
     *  - "<trackno>" (e.g. "12")
     */
    private val apiKey = BuildConfig.discogsApiKey
    private val apiSecret = BuildConfig.discogsApiSecret
    private val gson: Gson = GsonBuilder().create()
    private val responseCache = getMutexCache("DiscogsRepository.responseCache") { url ->
        val headers = mapOf(
            "User-Agent" to CUSTOM_USER_AGENT,
            "Authorization" to "Discogs key=$apiKey, secret=$apiSecret",
        )

        onIOThread {
            Request(url = url, headers = headers)
                .getString()
                .replace(Regex("^data\\((.*)\\)$"), "$1")
        }
    }

    suspend fun getMaster(masterId: Int): DiscogsMaster? =
        gson.fromJson(request("masters/$masterId"), DiscogsMaster::class.java)

    suspend fun searchMasters(query: String, artist: String? = null): DiscogsSearchResults? {
        val params = mutableMapOf("per_page" to "10", "query" to query, "type" to "master")
        if (artist != null) params["artist"] = artist
        return gson.fromJson(request("database/search", params), DiscogsSearchResults::class.java)
    }

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): String =
        responseCache.get(Request.getUrl("$API_ROOT/${path.trimStart('/')}", params.plus("callback" to "data")))

    companion object {
        const val API_ROOT = "https://api.discogs.com"
    }
}
