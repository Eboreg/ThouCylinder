package us.huseli.thoucylinder.repositories

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.SpotifyOAuth2
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.pojos.TopLocalSpotifyArtistPojo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumsResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtistsResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySearchResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTopArtistMatch
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendationResponse
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.spotify.toSpotifySavedAlbumResponse
import us.huseli.thoucylinder.distinctWith
import us.huseli.thoucylinder.fromJson
import us.huseli.thoucylinder.getMutexCache
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) {
    private val albumDao = database.albumDao()
    private val apiResponseCache = getMutexCache("SpotifyRepository.apiResponseCache") { url ->
        while (lastMinuteRequestCount >= REQUEST_LIMIT_PER_MINUTE) delay(1000)
        oauth2.getToken()?.accessToken?.let { accessToken ->
            Request(url = url, headers = mapOf("Authorization" to "Bearer $accessToken"))
                .getString()
                .also { requestTimes.value += System.currentTimeMillis() }
        }
    }
    private val lastMinuteRequestCount: Int
        get() {
            val oneMinuteAgo = System.currentTimeMillis().minus(60 * 1000)
            return requestTimes.value.filter { it >= oneMinuteAgo }.size
        }
    private val requestTimes = MutableStateFlow<List<Long>>(emptyList())

    private val _allUserAlbumsFetched = MutableStateFlow(false)
    private val _nextUserAlbumIdx = MutableStateFlow(0)
    private val _totalUserAlbumCount = MutableStateFlow<Int?>(null)
    private val _userAlbums = MutableStateFlow<List<SpotifyAlbum>>(emptyList())

    val oauth2 = SpotifyOAuth2(context)
    val allUserAlbumsFetched: StateFlow<Boolean> = _allUserAlbumsFetched.asStateFlow()
    val totalUserAlbumCount: StateFlow<Int?> = _totalUserAlbumCount.asStateFlow()
    val userAlbums: StateFlow<List<SpotifyAlbum>> = _userAlbums.asStateFlow()

    suspend fun getThumbnail(externalAlbum: SpotifyAlbum) =
        externalAlbum.images.toMediaStoreImage()?.getThumbnailImageBitmap(context)

    suspend fun fetchNextUserAlbums(): Boolean {
        if (!_allUserAlbumsFetched.value) {
            val url = "${API_ROOT}/me/albums?limit=50&offset=${_nextUserAlbumIdx.value}"

            apiResponseCache.getOrNull(url, retryOnNull = true)?.toSpotifySavedAlbumResponse()?.also { response ->
                _userAlbums.value += response.items.map { it.album }
                _nextUserAlbumIdx.value += response.items.size
                _allUserAlbumsFetched.value = response.next == null
                _totalUserAlbumCount.value = response.total
                return true
            }
        }
        return false
    }

    suspend fun getRelatedArtists(artistId: String): SpotifyArtistsResponse? {
        return apiResponseCache.getOrNull("$API_ROOT/artists/$artistId/related-artists")
            ?.fromJson<SpotifyArtistsResponse>()
    }

    suspend fun getSpotifyAlbums(albumIds: List<String>): SpotifyAlbumsResponse? {
        val url = Request.getUrl(
            url = "$API_ROOT/albums",
            params = mapOf("ids" to albumIds.joinToString(",")),
        )
        return apiResponseCache.getOrNull(url)?.fromJson<SpotifyAlbumsResponse>()
    }

    suspend fun getTopRelatedArtists(topLocalArtists: Collection<TopLocalSpotifyArtistPojo>): List<SpotifyTopArtistMatch> {
        val matches: List<SpotifyTopArtistMatch> = topLocalArtists.flatMap { artistPojo ->
            getRelatedArtists(artistPojo.spotifyId)?.let {
                it.artists.map { spotifyArtist ->
                    SpotifyTopArtistMatch(
                        artists = listOf(artistPojo.name),
                        spotifyArtist = spotifyArtist,
                        score = artistPojo.trackCount
                    )
                }
            } ?: emptyList()
        }
        return matches.distinctWith({ a, b ->
            a.copy(score = a.score + b.score, artists = a.artists.plus(b.artists))
        }) { it.spotifyArtist.id }.sortedByDescending { it.score }
    }

    suspend fun getTrackRecommendations(artistIds: List<String>): SpotifyTrackRecommendationResponse? {
        val url = Request.getUrl(
            url = "$API_ROOT/recommendations",
            params = mapOf("seed_artists" to artistIds.joinToString(",")),
        )
        return apiResponseCache.getOrNull(url)?.fromJson<SpotifyTrackRecommendationResponse>()
    }

    suspend fun listImportedAlbumIds() = albumDao.listImportedSpotifyIds()

    suspend fun searchAlbumArt(
        combo: AbstractAlbumCombo,
        getArtist: suspend (String) -> Artist,
    ): List<MediaStoreImage> = combo.album.spotifyImage?.let { image -> listOf(image) }
        ?: searchAlbums(combo)
            ?.albums
            ?.items
            ?.map { it.toAlbumCombo(getArtist = getArtist) }
            ?.filter { combo.getLevenshteinDistance(it) < 10 }
            ?.mapNotNull { it.album.albumArt }
        ?: emptyList()

    private fun getSearchQuery(params: Map<String, String?>): String = params.filterValuesNotNull()
        .map { (key, value) -> "$key:${URLEncoder.encode(value, "UTF-8")}" }
        .joinToString(" ")

    private suspend fun searchAlbums(combo: AbstractAlbumCombo): SpotifySearchResponse? {
        val params = mutableMapOf("album" to combo.album.title)
        combo.artists.joined()?.also { params["artist"] = it }
        val query = withContext(Dispatchers.IO) {
            URLEncoder.encode(getSearchQuery(params), "UTF-8")
        }
        val url = "$API_ROOT/search?q=$query&type=album"

        return apiResponseCache.getOrNull(url)?.fromJson<SpotifySearchResponse>()
    }

    companion object {
        // "Based on testing, we found that Spotify allows for approximately 180 requests per minute without returning
        // the error 429" -- https://community.spotify.com/t5/Spotify-for-Developers/Web-API-ratelimit/td-p/5330410
        // So let's be overly cautious.
        const val REQUEST_LIMIT_PER_MINUTE = 100
        const val API_ROOT = "https://api.spotify.com/v1"
    }
}
