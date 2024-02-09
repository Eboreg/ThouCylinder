package us.huseli.thoucylinder.repositories

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.Constants.COVERARTARCHIVE_API_ROOT
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.Constants.MUSICBRAINZ_API_ROOT
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveImage
import us.huseli.thoucylinder.dataclasses.CoverArtArchiveResponse
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzRelease
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseGroup
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseGroupSearch
import us.huseli.thoucylinder.dataclasses.musicBrainz.MusicBrainzReleaseSearch
import us.huseli.thoucylinder.dataclasses.musicBrainz.artistString
import us.huseli.thoucylinder.dataclasses.musicBrainz.toInternal
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.getString
import us.huseli.retaintheme.extensions.slice
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor() {
    private val gson: Gson = GsonBuilder().create()
    private val coverArtArchiveCache = MutexCache<String, CoverArtArchiveResponse> { releaseId ->
        val json = Request(
            url = "$COVERARTARCHIVE_API_ROOT/$releaseId",
            headers = mapOf("User-Agent" to CUSTOM_USER_AGENT),
        ).connect().getString()
        val response = gson.fromJson(
            json,
            CoverArtArchiveResponse::class.java,
        )
        Log.i(javaClass.simpleName, "json=$json")
        Log.i(javaClass.simpleName, "response=$response")
        response
    }
    private val musicBrainzReleaseCache = MutexCache<String, MusicBrainzRelease> { releaseId ->
        gson.fromJson(
            request("release/$releaseId", mapOf("inc" to "recordings artist-credits genres release-groups")),
            MusicBrainzRelease::class.java,
        )?.copy(id = releaseId)
    }
    private val musicBrainzReleaseGroupCache = MutexCache<String, MusicBrainzReleaseGroup> { groupId ->
        gson.fromJson(
            request("release-group/$groupId", mapOf("inc" to "artist-credits genres releases")),
            MusicBrainzReleaseGroup::class.java,
        )?.copy(id = groupId)
    }

    suspend fun getAllCoverArtArchiveImages(releaseId: String): List<CoverArtArchiveImage> =
        coverArtArchiveCache.get(releaseId)?.images?.filter { it.front } ?: emptyList()

    suspend fun getRelease(id: String): MusicBrainzRelease? = musicBrainzReleaseCache.get(id)

    suspend fun getReleaseCoverArt(releaseId: String): MediaStoreImage? = getRelease(releaseId)
        ?.let { getCoverArtArchiveImage(it) }
        ?.let { MediaStoreImage.fromUrls(it.image, it.thumbnails.thumb250) }

    suspend fun getSiblingReleaseIds(releaseId: String): List<String> =
        getRelease(releaseId)?.releaseGroup?.id?.let { getReleaseGroup(it) }?.releases?.map { it.id } ?: emptyList()

    fun matchAlbumWithTracks(
        combo: AlbumWithTracksCombo,
        release: MusicBrainzRelease,
    ): MusicBrainzRelease.AlbumMatch {
        val medium = release.getBestMediumMatch(combo)?.medium

        return MusicBrainzRelease.AlbumMatch(
            distance = release.getAlbumDistance(combo),
            albumCombo = combo.copy(
                album = combo.album.copy(
                    title = release.title,
                    year = release.year,
                    artist = release.artistCredit.artistString(),
                    musicBrainzReleaseId = release.id,
                    musicBrainzReleaseGroupId = release.releaseGroup.id,
                ),
                tracks = combo.tracks.mapIndexed { index, track ->
                    val mbTrack = medium?.tracks?.getOrNull(index)
                    track.copy(
                        musicBrainzId = mbTrack?.id,
                        artist = mbTrack?.artistCredit?.artistString() ?: track.artist,
                        year = mbTrack?.year ?: track.year,
                        title = mbTrack?.title ?: track.title,
                    )
                },
                genres = combo.genres.toSet().plus(release.allGenres.slice(0, 5).toInternal()).toList(),
            ),
        )
    }

    suspend fun matchAlbumWithTracks(combo: AlbumWithTracksCombo): AlbumWithTracksCombo? {
        val releaseIds = searchReleases(combo.album.title, combo.album.artist, combo.tracks.size)?.releaseIds
        val matches = releaseIds?.mapNotNull { releaseId ->
            getRelease(releaseId)?.let { release ->
                matchAlbumWithTracks(combo, release).takeIf { it.distance <= 1.0 }
            }
        }

        return matches?.minByOrNull { it.distance }?.albumCombo
    }

    private fun escapeString(string: String): String =
        string.replace(Regex("([+\\-&|!(){}\\[\\]^\"~*?:\\\\/])"), "\\\\$1")

    private suspend fun getCoverArtArchiveImage(release: MusicBrainzRelease): CoverArtArchiveImage? =
        getAllCoverArtArchiveImages(release.id).firstOrNull()
            ?: getReleaseGroup(release.releaseGroup.id)
                ?.releases
                ?.firstNotNullOfOrNull { getAllCoverArtArchiveImages(it.id).firstOrNull() }

    private suspend fun getReleaseGroup(id: String): MusicBrainzReleaseGroup? = musicBrainzReleaseGroupCache.get(id)

    private suspend fun request(path: String, params: Map<String, String> = emptyMap()): String {
        val paramString = withContext(Dispatchers.IO) {
            params.plus("fmt" to "json")
                .map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
                .joinToString("&")
                .takeIf { it.isNotEmpty() }
        }
        val url = "$MUSICBRAINZ_API_ROOT/${path.trimStart('/')}" + paramString?.let { "?$it" }
        val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)

        return Request(url = url, headers = headers).connect().getString()
    }

    private suspend fun search(path: String, params: Map<String, String> = emptyMap(), limit: Int = 10): String {
        val query = params
            .map { (key, value) -> "$key:${escapeString(value)}" }
            .joinToString(" AND ")

        return request(path, mapOf("query" to query, "limit" to limit.toString()))
    }

    @Suppress("unused")
    private suspend fun searchReleaseGroups(
        title: String,
        artist: String? = null,
        limit: Int = 10,
    ): MusicBrainzReleaseGroupSearch? {
        val params = mutableMapOf("release" to title)
        if (artist != null) params["artist"] = artist
        return gson.fromJson(search("release-group", params, limit), MusicBrainzReleaseGroupSearch::class.java)
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
}
