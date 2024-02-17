package us.huseli.thoucylinder.repositories

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveImage
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveResponse
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzRelease
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseGroup
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseSearch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor() {
    private val gson: Gson = GsonBuilder().create()
    private val apiResponseCache = MutexCache<String, String> { url ->
        Request(url = url, headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)).getString()
    }
    private val coverArtArchiveCache = MutexCache<String, CoverArtArchiveResponse> { releaseId ->
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
        ?.let { MediaStoreImage.fromUrls(it.image, it.thumbnails.thumb250) }

    suspend fun getReleaseId(combo: AlbumWithTracksCombo): String? =
        combo.album.musicBrainzReleaseId ?: matchAlbumWithTracks(combo)?.album?.musicBrainzReleaseId

    suspend fun getSiblingReleaseIds(releaseId: String): List<String> =
        getRelease(releaseId)?.releaseGroup?.id?.let { getReleaseGroup(it) }?.releases?.map { it.id } ?: emptyList()

    suspend fun listAllGenres(): Set<String> {
        val url = "$MUSICBRAINZ_API_ROOT/genre/all?fmt=txt"
        val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)

        return Request(url = url, headers = headers)
            .getString()
            .split('\n')
            .map { it.capitalized() }
            .toSet()
    }

    suspend fun matchAlbumWithTracks(combo: AlbumWithTracksCombo): AlbumWithTracksCombo? {
        val releaseIds = searchReleases(combo.album.title, combo.album.artist, combo.tracks.size)?.releaseIds
        val matches = releaseIds?.mapNotNull { releaseId ->
            getRelease(releaseId)?.let { release ->
                release.matchAlbumWithTracks(combo).takeIf { it.score <= MAX_ALBUM_MATCH_DISTANCE }
            }
        }

        return matches?.minByOrNull { it.score }?.albumCombo
    }

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

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): String =
        apiResponseCache.get(
            Request.getUrl("$MUSICBRAINZ_API_ROOT/${path.trimStart('/')}", params.plus("fmt" to "json"))
        )

    private suspend fun search(path: String, params: Map<String, String> = emptyMap(), limit: Int = 10): String {
        val query = params
            .map { (key, value) -> "$key:${escapeString(value)}" }
            .joinToString(" AND ")

        return request(path, mapOf("query" to query, "limit" to limit.toString()))
    }

    private suspend fun searchReleases(
        title: String,
        artist: String? = null,
        trackCount: Int? = null,
        limit: Int = 10,
    ): MusicBrainzReleaseSearch? {
        val params = mutableMapOf("release" to title)
        if (artist != null) params["artist"] = artist
        if (trackCount != null) params["tracks"] = trackCount.toString()
        return gson.fromJson(search("release", params, limit), MusicBrainzReleaseSearch::class.java)
    }

    companion object {
        const val MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
        const val COVERARTARCHIVE_API_ROOT = "https://coverartarchive.org/release"
        const val MAX_ALBUM_MATCH_DISTANCE = 1.0
    }
}
