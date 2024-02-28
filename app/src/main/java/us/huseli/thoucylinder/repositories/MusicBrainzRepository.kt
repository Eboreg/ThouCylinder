package us.huseli.thoucylinder.repositories

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveImage
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveResponse
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzArtistSearch
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzRelease
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseGroup
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseSearch
import us.huseli.thoucylinder.getMutexCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson: Gson = GsonBuilder().create()
    private var lastRequest: Long? = null
    private val apiResponseCache = getMutexCache("MusicBrainzRepository.apiResponseCache") { url ->
        lastRequest?.let { System.currentTimeMillis() - it }?.also { millisSinceLast ->
            if (millisSinceLast < MIN_REQUEST_INTERVAL_MS) {
                kotlinx.coroutines.delay(MIN_REQUEST_INTERVAL_MS - millisSinceLast)
            }
        }
        Request(url = url, headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)).getString()
            .also { lastRequest = System.currentTimeMillis() }
    }
    private val coverArtArchiveCache =
        getMutexCache<String, CoverArtArchiveResponse>("MusicBrainzRepository.coverArtArchiveCache") { releaseId ->
            Request(
                url = "$COVERARTARCHIVE_API_ROOT/$releaseId",
                headers = mapOf("User-Agent" to CUSTOM_USER_AGENT),
            ).getObject<CoverArtArchiveResponse>()
        }

    suspend fun getAllCoverArtArchiveImages(releaseId: String): List<CoverArtArchiveImage> =
        coverArtArchiveCache.getOrNull(releaseId)?.images?.filter { it.front } ?: emptyList()

    suspend fun getRelease(id: String): MusicBrainzRelease? = gson.fromJson(
        request("release/$id", mapOf("inc" to "recordings artist-credits genres release-groups")),
        MusicBrainzRelease::class.java,
    )?.copy(id = id)

    suspend fun getReleaseCoverArt(releaseId: String): MediaStoreImage? = getRelease(releaseId)
        ?.let { getCoverArtArchiveImage(it) }
        ?.let { MediaStoreImage.fromUrls(it.image, it.thumbnails.thumb250).nullIfNotFound(context) }

    suspend fun getReleaseId(combo: AlbumWithTracksCombo): String? = combo.album.musicBrainzReleaseId
        ?: getReleaseId(artist = combo.artists.joined(), album = combo.album.title, trackCount = combo.trackCount)

    suspend fun getSiblingReleaseIds(releaseId: String): List<String> =
        getRelease(releaseId)?.releaseGroup?.id?.let { getReleaseGroup(it) }?.releases?.map { it.id } ?: emptyList()

    suspend fun listAllGenreNames(): Set<String> {
        val url = "$MUSICBRAINZ_API_ROOT/genre/all?fmt=txt"
        val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)

        return Request(url = url, headers = headers)
            .getString()
            .split('\n')
            .map { it.capitalized() }
            .toSet()
    }

    suspend fun matchAlbumWithTracks(
        combo: AlbumWithTracksCombo,
        maxDistance: Double = MAX_ALBUM_MATCH_DISTANCE,
        strategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
        getArtist: suspend (String) -> Artist,
    ): AlbumWithTracksCombo? {
        val params = mutableMapOf(
            "release" to combo.album.title,
            "tracks" to combo.trackCombos.size.toString(),
        )
        combo.artists.joined()?.also { params["artist"] = it }
        val releaseIds =
            gson.fromJson(search("release", params), MusicBrainzReleaseSearch::class.java)?.releaseIds
        val matches = releaseIds?.mapNotNull { releaseId ->
            getRelease(releaseId)?.toAlbumWithTracks(isLocal = combo.album.isLocal, getArtist = getArtist)?.match(combo)
        }

        return matches?.filter { it.distance <= maxDistance }
            ?.minByOrNull { it.distance }
            ?.albumCombo
            ?.let { combo.updateWith(it, strategy) }
    }

    suspend fun matchArtist(artist: Artist): Artist? = try {
        if (artist.musicBrainzId == null) {
            gson.fromJson(search("artist", artist.name), MusicBrainzArtistSearch::class.java)
                ?.artists
                ?.firstOrNull { it.matches(artist.name) }
                ?.let { artist.copy(musicBrainzId = it.id) }
        } else null
    } catch (e: Exception) {
        null
    }


    /** PRIVATE METHODS *******************************************************/

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

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): String? {
        val url = Request.getUrl("$MUSICBRAINZ_API_ROOT/${path.trimStart('/')}", params.plus("fmt" to "json"))

        return apiResponseCache.getOrNull(url)
    }

    private suspend fun search(resource: String, params: Map<String, String> = emptyMap(), limit: Int = 10): String? {
        val query = params
            .map { (key, value) -> "$key:${escapeString(value)}" }
            .joinToString(" AND ")

        return request(resource, mapOf("query" to query, "limit" to limit.toString()))
    }

    private suspend fun search(resource: String, query: String, limit: Int = 10): String? {
        return request(resource, mapOf("query" to escapeString(query), "limit" to limit.toString()))
    }

    companion object {
        const val MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
        const val COVERARTARCHIVE_API_ROOT = "https://coverartarchive.org/release"
        const val MAX_ALBUM_MATCH_DISTANCE = 1.0
        // https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
        const val MIN_REQUEST_INTERVAL_MS = 1000L
    }
}
