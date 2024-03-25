package us.huseli.thoucylinder.repositories

import android.content.Context
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.retaintheme.extensions.slice
import us.huseli.thoucylinder.AbstractSpotifyOAuth2
import us.huseli.thoucylinder.DeferredRequestJob
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.SpotifyOAuth2ClientCredentials
import us.huseli.thoucylinder.SpotifyOAuth2PKCE
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.BaseArtist
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.spotify.AbstractSpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumsResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtistsResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySavedAlbumObject
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySearchResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackAudioFeaturesResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendationResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendations
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.fromJson
import us.huseli.thoucylinder.getNext
import us.huseli.thoucylinder.replaceNullPadding
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class SpotifyRepository @Inject constructor(database: Database, @ApplicationContext private val context: Context) {
    inner class RequestJob(
        url: String,
        private val oauth2: AbstractSpotifyOAuth2<*>,
        lowPrio: Boolean = false,
    ) : DeferredRequestJob(url, lowPrio) {
        override suspend fun request(): String? = oauth2.getAccessToken()?.let { accessToken ->
            return Request(url = url, headers = mapOf("Authorization" to "Bearer $accessToken")).getString()
        }

        override fun after(result: String?) {
            requestTimes.value += System.currentTimeMillis()
        }
    }

    private val albumDao = database.albumDao()
    private val apiResponseCache = MutexCache<RequestJob, String, String>(
        itemToKey = { it.url },
        fetchMethod = { job ->
            requestQueue.value += job
            job.run().also { requestQueue.value -= job }
        },
        debugLabel = "SpotifyRepository.apiResponseCache",
    )
    private var matchArtistsJob: Job? = null
    private val oauth2CC = SpotifyOAuth2ClientCredentials(context)
    private val requestTimes = MutableStateFlow<List<Long>>(emptyList())
    private val requestQueue = MutableStateFlow<List<RequestJob>>(emptyList())
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val spotifyDao = database.spotifyDao()

    private val _allUserAlbumsFetched = MutableStateFlow(false)
    private val _nextUserAlbumIdx = MutableStateFlow(0)
    private val _totalUserAlbumCount = MutableStateFlow<Int?>(null)
    private val _userAlbums = MutableStateFlow<List<SpotifyAlbum?>>(emptyList())

    val oauth2PKCE = SpotifyOAuth2PKCE(context)
    val allUserAlbumsFetched: StateFlow<Boolean> = _allUserAlbumsFetched.asStateFlow()
    val totalUserAlbumCount: StateFlow<Int?> = _totalUserAlbumCount.asStateFlow()
    val userAlbums: StateFlow<List<SpotifyAlbum?>> = _userAlbums.asStateFlow()

    init {
        scope.launch {
            requestQueue.collect { jobs ->
                jobs.getNext()?.also { job ->
                    // If more than the allowed number of requests have been made in the last minute: wait until this
                    // is not the case.
                    val now = System.currentTimeMillis()
                    val lastMinuteRequestDistances = requestTimes.value.map { now - it }.filter { it < 60_000L }

                    if (lastMinuteRequestDistances.size >= REQUEST_LIMIT_PER_MINUTE) {
                        val delayMillis = 60_000L - lastMinuteRequestDistances
                            .slice(0, lastMinuteRequestDistances.size - REQUEST_LIMIT_PER_MINUTE)
                            .last()

                        delay(delayMillis)
                    }

                    job.lock.unlock()
                }
            }
        }

        scope.launch {
            spotifyDao.flowSpotifyTrackIdsWithoutAudioFeatures()
                .takeWhile { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { spotifyTrackIds ->
                    for (chunk in spotifyTrackIds.chunked(100)) {
                        val job = RequestJob(
                            url = "${API_ROOT}/audio-features?ids=${chunk.joinToString(",")}",
                            oauth2 = oauth2CC,
                            lowPrio = true,
                        )

                        apiResponseCache.getOrNull(job)
                            ?.fromJson<SpotifyTrackAudioFeaturesResponse>()
                            ?.audioFeatures
                            ?.filterNotNull()
                            ?.also { spotifyDao.insertAudioFeatures(it) }
                    }
                }
        }
    }

    suspend fun fetchNextUserAlbums(): Boolean {
        if (!_allUserAlbumsFetched.value) {
            val job = RequestJob(
                url = "${API_ROOT}/me/albums?limit=50&offset=${_nextUserAlbumIdx.value}",
                oauth2 = oauth2PKCE,
            )

            apiResponseCache.getOrNull(job, retryOnNull = true)
                ?.fromJson(object : TypeToken<SpotifyResponse<SpotifySavedAlbumObject>>() {})
                ?.also { response ->
                    val userAlbums =
                        _userAlbums.value.replaceNullPadding(response.offset, response.items.map { it.album })
                    _nextUserAlbumIdx.value = userAlbums.size
                    _userAlbums.value = userAlbums
                    _allUserAlbumsFetched.value = response.next == null
                    _totalUserAlbumCount.value = response.total
                    return true
                }
        }
        return false
    }

    suspend fun getTrackRecommendations(spotifyTrackIds: Collection<String>, limit: Int): SpotifyTrackRecommendations {
        val newTracks = mutableListOf<SpotifyTrack>()
        val innerLimit = min(limit * 2, 100)

        if (spotifyTrackIds.isNotEmpty()) {
            while (newTracks.size < limit) {
                val seed = spotifyTrackIds.shuffled().take(5)
                val recommendations = getTrackRecommendations(
                    params = mapOf("seed_tracks" to seed.joinToString(",")),
                    limit = innerLimit,
                )

                newTracks.addAll(
                    recommendations.tracks
                        .filter { !spotifyTrackIds.contains(it.id) }
                        .take(limit - newTracks.size)
                )
                if (!recommendations.hasMore)
                    return SpotifyTrackRecommendations(tracks = newTracks, requestedTracks = limit)
            }
        }

        return SpotifyTrackRecommendations(tracks = newTracks, requestedTracks = limit)
    }

    suspend fun getRelatedArtists(artistId: String, limit: Int = 10): List<SpotifyArtist>? =
        apiResponseCache.getOrNull(RequestJob(url = "$API_ROOT/artists/$artistId/related-artists", oauth2 = oauth2CC))
            ?.fromJson<SpotifyArtistsResponse>()
            ?.artists
            ?.sortedByDescending { it.popularity }
            ?.slice(0, limit)

    suspend fun getSpotifyAlbums(albumIds: List<String>): List<SpotifyAlbum>? {
        val job = RequestJob(
            url = Request.getUrl(
                url = "$API_ROOT/albums",
                params = mapOf("ids" to albumIds.joinToString(",")),
            ),
            oauth2 = oauth2CC,
        )
        return apiResponseCache.getOrNull(job)?.fromJson<SpotifyAlbumsResponse>()?.albums
    }

    suspend fun getThumbnail(externalAlbum: SpotifyAlbum) =
        externalAlbum.images.toMediaStoreImage()?.getThumbnailImageBitmap(context)

    suspend fun getTrackRecommendationsByAlbumCombo(
        albumCombo: AlbumWithTracksCombo,
        limit: Int,
    ): SpotifyTrackRecommendations? {
        val allTrackIds = albumCombo.trackCombos.mapNotNull { it.track.spotifyId }.takeIf { it.isNotEmpty() }
            ?: (albumCombo.album.spotifyId ?: matchAlbumCombo(albumCombo)?.id)
                ?.let { getSpotifyAlbum(it) }?.tracks?.items?.map { it.id }
            ?: return null
        val trackIds = allTrackIds.shuffled().take(5)
        val albumArtistIds =
            if (trackIds.size < 5) albumCombo.artists.mapNotNull { it.spotifyId }.take(5 - trackIds.size)
            else emptyList()

        return getTrackRecommendations(
            params = mapOf(
                "seed_tracks" to trackIds.joinToString(","),
                "seed_artists" to albumArtistIds.joinToString(","),
            ),
            limit = limit,
        )
    }

    suspend fun getTrackRecommendationsByArtist(artist: Artist, limit: Int): SpotifyTrackRecommendations? =
        (artist.spotifyId ?: matchArtist(artist.name)?.id)?.let { spotifyId ->
            getTrackRecommendations(params = mapOf("seed_artists" to spotifyId), limit = limit)
        }

    suspend fun getTrackRecommendationsByTrack(
        track: Track,
        album: Album? = null,
        artists: List<Artist> = emptyList(),
        limit: Int,
    ): SpotifyTrackRecommendations? {
        val spotifyId = track.spotifyId ?: matchTrack(track, album, artists)?.id

        return spotifyId?.let { getTrackRecommendations(params = mapOf("seed_tracks" to it), limit = limit) }
    }

    suspend fun listImportedAlbumIds() = albumDao.listImportedSpotifyIds()

    suspend fun matchArtist(name: String, lowPrio: Boolean = false): SpotifyArtist? =
        search("artist", mapOf("artist" to name), lowPrio)
            ?.artists
            ?.items
            ?.firstOrNull { it.name.lowercase() == name.lowercase() }

    suspend fun searchAlbumArt(
        combo: AbstractAlbumCombo,
        getArtist: suspend (BaseArtist) -> Artist,
    ): List<MediaStoreImage> = combo.album.spotifyImage?.let { image -> listOf(image) }
        ?: searchAlbums(combo)
            ?.albums
            ?.items
            ?.map { it.toAlbumCombo(getArtist = getArtist) }
            ?.filter { combo.getLevenshteinDistance(it) < 10 }
            ?.mapNotNull { it.album.albumArt }
        ?: emptyList()

    fun startMatchingArtists(flow: Flow<List<Artist>>, save: suspend (UUID, String, MediaStoreImage?) -> Unit) {
        if (matchArtistsJob == null) matchArtistsJob = scope.launch {
            val previousIds = mutableSetOf<UUID>()

            flow
                .map { artists -> artists.filter { it.spotifyId == null && !previousIds.contains(it.id) } }
                .collect { artists ->
                    for (artist in artists) {
                        val match = matchArtist(artist.name, true)

                        save(artist.id, match?.id ?: "", match?.images?.toMediaStoreImage())
                        previousIds.add(artist.id)
                    }
                }
        }
    }

    fun unauthorize() {
        oauth2PKCE.clearToken()
        _userAlbums.value = emptyList()
        _nextUserAlbumIdx.value = 0
    }


    /** PRIVATE METHODS *******************************************************/

    private fun getSearchQuery(params: Map<String, String?>): String = params.filterValuesNotNull()
        .map { (key, value) -> "$key:${URLEncoder.encode(value, "UTF-8")}" }
        .joinToString(" ")

    private suspend fun getSpotifyAlbum(albumId: String): SpotifyAlbum? =
        getSpotifyAlbums(listOf(albumId))?.firstOrNull()

    private suspend fun getTrackRecommendations(
        params: Map<String, String>,
        limit: Int,
    ): SpotifyTrackRecommendations {
        val job = RequestJob(
            url = Request.getUrl(
                url = "$API_ROOT/recommendations",
                params = params.plus("limit" to limit.toString())
            ),
            oauth2 = oauth2CC,
        )
        val response = apiResponseCache.getOrNull(job)?.fromJson<SpotifyTrackRecommendationResponse>()

        return SpotifyTrackRecommendations(tracks = response?.tracks ?: emptyList(), requestedTracks = limit)
    }

    private suspend fun matchAlbumCombo(albumCombo: AbstractAlbumCombo): AbstractSpotifyAlbum? {
        val params = mutableMapOf("album" to albumCombo.album.title)

        albumCombo.artists.joined()?.also { params["artist"] = it }

        return search("album", params)
            ?.albums
            ?.items
            ?.map { it.matchAlbumCombo(albumCombo) }
            ?.filter { it.distance <= 5 }
            ?.minByOrNull { it.distance }
            ?.spotifyAlbum
    }

    suspend fun matchTrack(
        track: Track,
        album: Album? = null,
        artists: Collection<Artist> = emptyList(),
    ): SpotifyTrack? {
        val params = mutableMapOf("track" to track.title)

        if (artists.isNotEmpty()) params["artist"] = artists.map { it.name }.toSet().joinToString(", ")
        album?.also { params["album"] = it.title }

        return search("track", params)
            ?.tracks
            ?.items
            ?.map { it.matchTrack(track, album, artists) }
            ?.filter { it.distance <= 10 }
            ?.minByOrNull { it.distance }
            ?.spotifyTrack
    }

    private suspend fun searchAlbums(combo: AbstractAlbumCombo): SpotifySearchResponse? {
        val params = mutableMapOf("album" to combo.album.title)

        combo.artists.joined()?.also { params["artist"] = it }
        return search("album", params)
    }

    private suspend fun search(
        type: String,
        params: Map<String, String>,
        lowPrio: Boolean = false,
    ): SpotifySearchResponse? {
        val query = withContext(Dispatchers.IO) {
            URLEncoder.encode(getSearchQuery(params), "UTF-8")
        }
        val job = RequestJob(url = "$API_ROOT/search?q=$query&type=$type", oauth2 = oauth2CC, lowPrio = lowPrio)

        return apiResponseCache.getOrNull(job)?.fromJson<SpotifySearchResponse>()
    }

    companion object {
        // "Based on testing, we found that Spotify allows for approximately 180 requests per minute without returning
        // the error 429" -- https://community.spotify.com/t5/Spotify-for-Developers/Web-API-ratelimit/td-p/5330410
        // So let's be overly cautious.
        const val REQUEST_LIMIT_PER_MINUTE = 100
        const val API_ROOT = "https://api.spotify.com/v1"
    }
}
