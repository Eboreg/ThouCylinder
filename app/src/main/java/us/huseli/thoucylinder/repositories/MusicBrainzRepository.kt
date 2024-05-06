package us.huseli.thoucylinder.repositories

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.DeferredRequestJob
import us.huseli.thoucylinder.DeferredRequestJobManager
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveImage
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveResponse
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzArtistSearch
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzRelease
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseGroup
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseSearch
import us.huseli.thoucylinder.getMutexCache
import us.huseli.thoucylinder.interfaces.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor() : AbstractScopeHolder() {
    class RequestJob(url: String, lowPrio: Boolean = false) : DeferredRequestJob(url, lowPrio) {
        override suspend fun request(): String =
            Request(url = url, headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)).getString()
    }

    object RequestJobManager : DeferredRequestJobManager<RequestJob>(), ILogger {
        private var lastJobFinished: Long? = null

        override suspend fun waitBeforeUnlocking() {
            val timeSinceLast = lastJobFinished?.let { System.currentTimeMillis() - it }

            if (timeSinceLast != null && timeSinceLast < MIN_REQUEST_INTERVAL_MS) {
                log("timeSinceLast == $timeSinceLast, will delay for ${MIN_REQUEST_INTERVAL_MS - timeSinceLast} ms")
                delay(MIN_REQUEST_INTERVAL_MS - timeSinceLast)
            }
        }

        override fun onJobFinished(job: RequestJob, response: String?, timestamp: Long) {
            lastJobFinished = timestamp
        }
    }

    private val gson: Gson = GsonBuilder().create()
    private val apiResponseCache = MutexCache(
        itemToKey = { it.url },
        fetchMethod = { RequestJobManager.runJob(it) },
        debugLabel = "MusicBrainzRepository.apiResponseCache",
    )
    private val coverArtArchiveCache =
        getMutexCache<String, CoverArtArchiveResponse>("MusicBrainzRepository.coverArtArchiveCache") { releaseId ->
            Request(
                url = "$COVERARTARCHIVE_API_ROOT/$releaseId",
                headers = mapOf("User-Agent" to CUSTOM_USER_AGENT),
            ).getObject<CoverArtArchiveResponse>()
        }
    private var matchArtistsJob: Job? = null

    suspend fun getAllCoverArtArchiveImages(releaseId: String): List<CoverArtArchiveImage> =
        coverArtArchiveCache.getOrNull(releaseId)?.images?.filter { it.front } ?: emptyList()

    suspend fun getRelease(id: String): MusicBrainzRelease? = gson.fromJson(
        request("release/$id", mapOf("inc" to "recordings artist-credits genres release-groups")),
        MusicBrainzRelease::class.java,
    )?.copy(id = id)

    suspend fun getReleaseCoverArt(releaseId: String): MediaStoreImage? = getRelease(releaseId)
        ?.let { getCoverArtArchiveImage(it)?.toMediaStoreImage() }

    suspend fun getReleaseId(combo: AlbumWithTracksCombo): String? = combo.album.musicBrainzReleaseId
        ?: getReleaseId(artist = combo.artists.joined(), album = combo.album.title, trackCount = combo.trackCount)

    suspend fun getSiblingReleaseIds(releaseId: String): List<String> =
        getRelease(releaseId)?.releaseGroup?.id?.let { getReleaseGroup(it) }?.releases?.map { it.id } ?: emptyList()

    suspend fun listAllGenreNames(): Set<String> {
        val url = "$MUSICBRAINZ_API_ROOT/genre/all?fmt=txt"
        val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)

        return Request(url = url, headers = headers).getString().split('\n').toSet()
    }

    suspend fun matchAlbumWithTracks(
        combo: AlbumWithTracksCombo,
        maxDistance: Double = MAX_ALBUM_MATCH_DISTANCE,
        strategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
        getArtist: suspend (UnsavedArtist) -> Artist,
    ): AlbumWithTracksCombo? {
        val params = mutableMapOf(
            "release" to combo.album.title,
            "tracks" to combo.trackCombos.size.toString(),
        )
        combo.artists.joined()?.also { params["artist"] = it }
        val releaseIds =
            gson.fromJson(search("release", params), MusicBrainzReleaseSearch::class.java)?.releaseIds
        val matches = releaseIds?.mapNotNull { releaseId ->
            getRelease(releaseId)?.toAlbumWithTracks(
                isLocal = combo.album.isLocal,
                getArtist = getArtist,
                isInLibrary = combo.album.isInLibrary,
            )?.match(combo)
        }

        return matches?.filter { it.distance <= maxDistance }
            ?.minByOrNull { it.distance }
            ?.albumCombo
            ?.let { combo.updateWith(it, strategy) }
    }

    fun startMatchingArtists(flow: Flow<List<Artist>>, save: suspend (String, String) -> Unit) {
        if (matchArtistsJob == null) matchArtistsJob = launchOnIOThread {
            val previousIds = mutableSetOf<String>()

            flow
                .map { artists -> artists.filter { it.musicBrainzId == null && !previousIds.contains(it.artistId) } }
                .collect { artists ->
                    for (artist in artists) {
                        val matchedId = search("artist", artist.name, true)
                            ?.let { gson.fromJson(it, MusicBrainzArtistSearch::class.java) }
                            ?.artists
                            ?.firstOrNull { it.matches(artist.name) }
                            ?.id

                        save(artist.artistId, matchedId ?: "")
                        previousIds.add(artist.artistId)
                    }
                }
        }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun escapeString(string: String): String =
        string.replace(Regex("([+\\-&|!(){}\\[\\]^\"~*?:\\\\/])"), "\\\\$1")

    private suspend fun getCoverArtArchiveImage(release: MusicBrainzRelease): CoverArtArchiveImage? =
        getAllCoverArtArchiveImages(release.id).firstOrNull()
            ?: getReleaseGroup(release.releaseGroup.id)
                ?.releases
                ?.firstNotNullOfOrNull { getAllCoverArtArchiveImages(it.id).firstOrNull() }

    private suspend fun getReleaseGroup(id: String): MusicBrainzReleaseGroup? = gson.fromJson(
        request("release-group/$id", mapOf("inc" to "artist-credits genres releases")),
        MusicBrainzReleaseGroup::class.java,
    )?.copy(id = id)

    private suspend fun getReleaseId(artist: String?, album: String, trackCount: Int): String? {
        val params = mutableMapOf(
            "release" to album,
            "tracks" to trackCount.toString(),
        )
        if (artist != null) params["artist"] = artist
        val response = gson.fromJson(search("release", params), MusicBrainzReleaseSearch::class.java)

        return response?.releases?.firstOrNull { release -> release.matches(artist, album) }?.id
    }

    private suspend fun request(
        path: String,
        params: Map<String, String> = emptyMap(),
        lowPrio: Boolean = false,
    ): String? {
        val url = Request.getUrl("$MUSICBRAINZ_API_ROOT/${path.trimStart('/')}", params.plus("fmt" to "json"))

        return apiResponseCache.getOrNull(RequestJob(url, lowPrio))
    }

    private suspend fun search(
        resource: String,
        queryParams: Map<String, String> = emptyMap(),
        lowPrio: Boolean = false,
        limit: Int = 10,
    ): String? {
        val query = queryParams
            .map { (key, value) -> "$key:${escapeString(value)}" }
            .joinToString(" AND ")

        return request(resource, mapOf("query" to query, "limit" to limit.toString()), lowPrio)
    }

    private suspend fun search(resource: String, query: String, lowPrio: Boolean = false, limit: Int = 10): String? {
        return request(resource, mapOf("query" to escapeString(query), "limit" to limit.toString()), lowPrio)
    }

    companion object {
        const val MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
        const val COVERARTARCHIVE_API_ROOT = "https://coverartarchive.org/release"
        const val MAX_ALBUM_MATCH_DISTANCE = 1.0
        // https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
        const val MIN_REQUEST_INTERVAL_MS = 1000L
    }
}
