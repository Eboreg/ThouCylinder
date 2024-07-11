package us.huseli.thoucylinder.repositories

import android.content.Context
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.AbstractSpotifyOAuth2
import us.huseli.thoucylinder.DeferredRequestJob
import us.huseli.thoucylinder.DeferredRequestJobManager
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.SpotifyOAuth2ClientCredentials
import us.huseli.thoucylinder.SpotifyOAuth2PKCE
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.artist.IArtist
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.artist.names
import us.huseli.thoucylinder.dataclasses.spotify.AbstractSpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumType
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumsResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtistsResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySavedAlbumObject
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySearchResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySimplifiedAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackAudioFeatures
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackAudioFeaturesResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendationResponse
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrackRecommendations
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyUserProfile
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.enums.ListUpdateStrategy
import us.huseli.thoucylinder.externalcontent.SearchParams
import us.huseli.thoucylinder.fromJson
import us.huseli.thoucylinder.interfaces.ILogger
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), ILogger {
    inner class RequestJob(
        url: String,
        private val oauth2: AbstractSpotifyOAuth2<*>,
        lowPrio: Boolean = false,
    ) : DeferredRequestJob(url, lowPrio) {
        override suspend fun request(): String? = oauth2.getAccessToken()?.let { accessToken ->
            return Request(url = url, headers = mapOf("Authorization" to "Bearer $accessToken")).getString()
        }
    }

    object RequestJobManager : DeferredRequestJobManager<RequestJob>() {
        private val requestTimes = MutableStateFlow<List<Long>>(emptyList())

        override suspend fun waitBeforeUnlocking() {
            // If more than the allowed number of requests have been made in the last minute: wait until this
            // is not the case.
            val now = System.currentTimeMillis()
            val lastMinuteRequestDistances = requestTimes.value.map { now - it }.filter { it < 60_000L }

            if (lastMinuteRequestDistances.size >= REQUEST_LIMIT_PER_MINUTE) {
                val delayMillis = 60_000L - lastMinuteRequestDistances
                    .take(lastMinuteRequestDistances.size - REQUEST_LIMIT_PER_MINUTE)
                    .last()

                delay(delayMillis)
            }
        }

        override fun onJobFinished(job: RequestJob, response: String?, timestamp: Long) {
            requestTimes.value += timestamp
        }
    }

    private val albumDao = database.albumDao()
    private val apiResponseCache = MutexCache<RequestJob, String, String>(
        itemToKey = { it.url },
        fetchMethod = { RequestJobManager.runJob(it) },
        debugLabel = "SpotifyRepository.apiResponseCache",
    )
    private var matchArtistsJob: Job? = null
    private val oauth2CC = SpotifyOAuth2ClientCredentials(context)
    private val spotifyDao = database.spotifyDao()

    private val _allUserAlbumsFetched = MutableStateFlow(false)
    private val _totalUserAlbumCount = MutableStateFlow<Int?>(null)

    val oauth2PKCE = SpotifyOAuth2PKCE(context)
    val allUserAlbumsFetched: StateFlow<Boolean> = _allUserAlbumsFetched.asStateFlow()
    val totalUserAlbumCount: StateFlow<Int?> = _totalUserAlbumCount.asStateFlow()

    init {
        launchOnIOThread {
            spotifyDao.flowSpotifyTrackIdsWithoutAudioFeatures()
                .takeWhile { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { spotifyTrackIds ->
                    for (chunk in spotifyTrackIds.chunked(100)) {
                        getAudioFeatures(chunk)
                    }
                }
        }
        launchOnIOThread {
            oauth2PKCE.token.collect {
                apiResponseCache.clear()
            }
        }
    }

    fun albumSearchChannel(searchParams: SearchParams) = Channel<SpotifySimplifiedAlbum>().also { channel ->
        launchOnIOThread {
            val params = mutableMapOf<String, String>()

            if (!searchParams.album.isNullOrEmpty()) params["album"] = searchParams.album
            if (!searchParams.artist.isNullOrEmpty()) params["artist"] = searchParams.artist

            if (params.isNotEmpty() || !searchParams.freeText.isNullOrBlank()) {
                var url: String? =
                    getSearchUrl(type = "album", params = params, limit = 50, freeText = searchParams.freeText)

                while (url != null) {
                    val albumResponse = searchByUrl(url)?.albums

                    if (albumResponse != null) {
                        url = albumResponse.next
                        for (album in albumResponse.items) {
                            channel.send(album)
                        }
                    } else url = null
                }
            }
            channel.close()
        }
    }

    fun flowArtistAlbums(
        artistId: String,
        albumTypes: Collection<SpotifyAlbumType> = SpotifyAlbumType.entries,
        limit: Int? = null,
    ) = channelFlow<SpotifySimplifiedAlbum> {
        val includeGroups = albumTypes.joinToString(",") { it.name.lowercase() }
        val requestLimit = limit?.coerceAtMost(50) ?: 50
        var url: String? = Request.getUrl(
            url = "${API_ROOT}/artists/$artistId/albums",
            params = mapOf("include_groups" to includeGroups, "limit" to requestLimit.toString()),
        )
        var added = 0

        while (url != null && (limit == null || added < limit)) {
            val job = RequestJob(url = url, oauth2 = oauth2CC)

            apiResponseCache.getOrNull(job, retryOnNull = true)
                ?.fromJson(object : TypeToken<SpotifyResponse<SpotifySimplifiedAlbum>>() {})
                ?.also { response ->
                    url = response.next
                    response.items.forEach { send(it) }
                    added += response.items.size
                }
        }
    }

    fun flowTrackAudioFeatures(spotifyTrackId: String): Flow<SpotifyTrackAudioFeatures?> = flow {
        emit(null)
        spotifyDao.flowAudioFeatures(spotifyTrackId).collect { features ->
            emit(features)
            if (features == null) getAudioFeatures(listOf(spotifyTrackId))?.firstOrNull()?.also { emit(it) }
        }
    }

    suspend fun getRelatedArtists(artistId: String, limit: Int = 10): List<SpotifyArtist>? =
        apiResponseCache.getOrNull(RequestJob(url = "$API_ROOT/artists/$artistId/related-artists", oauth2 = oauth2CC))
            ?.fromJson<SpotifyArtistsResponse>()
            ?.artists
            ?.sortedByDescending { it.popularity }
            ?.take(limit)

    suspend fun getAlbum(albumId: String): SpotifyAlbum? = getAlbums(listOf(albumId))?.firstOrNull()

    suspend fun getAlbums(albumIds: List<String>): List<SpotifyAlbum>? {
        val job = RequestJob(
            url = Request.getUrl(
                url = "$API_ROOT/albums",
                params = mapOf("ids" to albumIds.joinToString(",")),
            ),
            oauth2 = oauth2CC,
        )
        return apiResponseCache.getOrNull(job)?.fromJson<SpotifyAlbumsResponse>()?.albums
    }

    suspend fun getUserProfile(): SpotifyUserProfile? {
        return RequestJobManager.runJob(RequestJob(url = "$API_ROOT/me", oauth2 = oauth2PKCE))
            ?.fromJson<SpotifyUserProfile>()
    }

    suspend fun listSpotifyAlbumIds(): List<String> = albumDao.listSpotifyAlbumIds()

    suspend fun matchAlbumWithTracks(
        combo: IAlbumWithTracksCombo<IAlbum>,
        maxDistance: Double = MAX_ALBUM_MATCH_DISTANCE,
        trackMergeStrategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
        albumArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        trackArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        tagUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE,
    ): UnsavedAlbumWithTracksCombo? {
        val spotifyAlbum = getBestAlbumMatch(combo.album.title, combo.artists)
        val match = spotifyAlbum?.toAlbumWithTracks(
            isLocal = combo.album.isLocal,
            isInLibrary = combo.album.isInLibrary,
        )?.match(combo)

        return match?.takeIf { it.distance <= maxDistance }?.albumCombo?.let {
            combo.updateWith(
                other = it,
                trackMergeStrategy = trackMergeStrategy,
                albumArtistUpdateStrategy = albumArtistUpdateStrategy,
                trackArtistUpdateStrategy = trackArtistUpdateStrategy,
                tagUpdateStrategy = tagUpdateStrategy,
            )
        }
    }

    suspend fun matchTrack(
        track: Track,
        album: Album? = null,
        artists: Collection<IArtist> = emptyList(),
    ): SpotifyTrack? {
        val params = mutableMapOf("track" to track.title)
        val artistNames = artists.map { it.name }

        if (artists.isNotEmpty()) params["artist"] = artistNames.toSet().joinToString(", ")
        album?.also { params["album"] = it.title }

        return search(type = "track", params = params)
            ?.tracks
            ?.items
            ?.map { it.matchTrack(track, album, artistNames) }
            ?.filter { it.distance <= 10 }
            ?.minByOrNull { it.distance }
            ?.spotifyTrack
    }

    suspend fun searchAlbumArt(album: Album, artistString: String?): List<MediaStoreImage> =
        album.spotifyImage?.let { image -> listOf(image) }
            ?: searchAlbums(album.title, artistString)
                ?.albums
                ?.items
                ?.map { it.toAlbumCombo(isLocal = false, isInLibrary = false) }
                ?.filter { it.getLevenshteinDistance(album.title, artistString) < 10 }
                ?.mapNotNull { it.album.albumArt }
            ?: emptyList()

    fun startMatchingArtists(flow: Flow<List<Artist>>, save: suspend (String, String, MediaStoreImage?) -> Unit) {
        if (matchArtistsJob == null) matchArtistsJob = launchOnIOThread {
            val previousIds = mutableSetOf<String>()

            flow
                .map { artists -> artists.filter { it.spotifyId == null && !previousIds.contains(it.artistId) } }
                .collect { artists ->
                    for (artist in artists) {
                        val match = matchArtist(artist.name, true)

                        save(artist.artistId, match?.id ?: "", match?.images?.toMediaStoreImage())
                        previousIds.add(artist.artistId)
                    }
                }
        }
    }

    fun trackRecommendationsChannelByAlbumCombo(combo: AlbumWithTracksCombo) = Channel<SpotifyTrack>().also { channel ->
        launchOnIOThread {
            val allTrackIds = combo.trackCombos
                .mapNotNull { it.track.spotifyId }
                .takeIf { it.isNotEmpty() }
                ?: (combo.album.spotifyId ?: matchAlbumCombo(combo.album.title, combo.artists)?.id)
                    ?.let { getAlbum(it) }?.tracks?.items?.map { it.id }
            val trackIds = allTrackIds?.shuffled()?.take(5) ?: emptyList()
            val albumArtistIds =
                if (trackIds.size < 5) combo.artists.mapNotNull { it.spotifyId }.take(5 - trackIds.size)
                else emptyList()
            val recommendations = if (trackIds.isNotEmpty() || albumArtistIds.isNotEmpty()) getTrackRecommendations(
                params = mapOf(
                    "seed_tracks" to trackIds.joinToString(","),
                    "seed_artists" to albumArtistIds.joinToString(",")
                ),
                limit = 40,
            ) else null

            if (recommendations != null) {
                for (track in recommendations.tracks) {
                    channel.send(track)
                }
                for (track in trackRecommendationsChannel(recommendations.tracks.map { it.id })) {
                    channel.send(track)
                }
            }
            channel.close()
        }
    }

    fun trackRecommendationsChannelByArtist(artist: Artist) = Channel<SpotifyTrack>().also { channel ->
        launchOnIOThread {
            val artistId = artist.spotifyId ?: matchArtist(artist.name)?.id
            val recommendations = artistId?.let {
                getTrackRecommendations(params = mapOf("seed_artists" to it), limit = 40)
            }

            if (recommendations != null) {
                for (track in recommendations.tracks) {
                    channel.send(track)
                }
                for (track in trackRecommendationsChannel(recommendations.tracks.map { it.id })) {
                    channel.send(track)
                }
            }
            channel.close()
        }
    }

    fun trackRecommendationsChannelByTrack(
        track: Track,
        album: Album? = null,
        artists: List<IArtist> = emptyList(),
    ) = Channel<SpotifyTrack>().also { channel ->
        launchOnIOThread {
            val trackId = track.spotifyId ?: matchTrack(track, album, artists)?.id
            val recommendations = trackId?.let {
                getTrackRecommendations(params = mapOf("seed_tracks" to it), limit = 40)
            }

            if (recommendations != null) {
                for (spotifyTrack in recommendations.tracks) {
                    channel.send(spotifyTrack)
                }
                for (spotifyTrack in trackRecommendationsChannel(recommendations.tracks.map { it.id })) {
                    channel.send(spotifyTrack)
                }
            }
            channel.close()
        }
    }

    fun trackRecommendationsChannel(usedTrackIds: Collection<String>) = Channel<SpotifyTrack>().also { channel ->
        launchOnIOThread {
            val mutableUsedTrackIds = usedTrackIds.toMutableList()

            do {
                val seed = mutableUsedTrackIds.shuffled().take(5)
                val recommendations = getTrackRecommendations(
                    params = mapOf("seed_tracks" to seed.joinToString(",")),
                    limit = 40,
                )

                for (track in recommendations.tracks.filter { !mutableUsedTrackIds.contains(it.id) }) {
                    channel.send(track)
                    mutableUsedTrackIds.add(track.id)
                }
            } while (recommendations.hasMore)

            channel.close()
        }
    }

    fun trackSearchChannel(searchParams: SearchParams) = Channel<SpotifyTrack>().also { channel ->
        launchOnIOThread {
            val params = mutableMapOf<String, String>()

            if (!searchParams.track.isNullOrEmpty()) params["track"] = searchParams.track
            if (!searchParams.album.isNullOrEmpty()) params["album"] = searchParams.album
            if (!searchParams.artist.isNullOrEmpty()) params["artist"] = searchParams.artist

            if (params.isNotEmpty() || !searchParams.freeText.isNullOrBlank()) {
                var url: String? =
                    getSearchUrl(type = "track", params = params, limit = 50, freeText = searchParams.freeText)

                while (url != null) {
                    val trackResponse = searchByUrl(url)?.tracks

                    if (trackResponse != null) {
                        url = trackResponse.next
                        for (track in trackResponse.items) channel.send(track)
                    } else {
                        url = null
                    }
                }
            }
            channel.close()
        }
    }

    fun unauthorize() {
        oauth2PKCE.clearToken()
    }

    fun userAlbumsChannel() = Channel<SpotifyAlbum>().also { channel ->
        launchOnIOThread {
            var url: String? = "${API_ROOT}/me/albums?limit=50&offset=0"

            while (url != null) {
                val job = RequestJob(url = url, oauth2 = oauth2PKCE)

                apiResponseCache.getOrNull(job, retryOnNull = true)
                    ?.fromJson(object : TypeToken<SpotifyResponse<SpotifySavedAlbumObject>>() {})
                    ?.also { response ->
                        url = response.next
                        _allUserAlbumsFetched.value = response.next == null
                        _totalUserAlbumCount.value = response.total
                        response.items.map { it.album }.forEach {
                            channel.send(it)
                        }
                    }
            }
        }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun getAudioFeatures(spotifyTrackIds: Collection<String>): List<SpotifyTrackAudioFeatures>? {
        val job = RequestJob(
            url = "${API_ROOT}/audio-features?ids=${spotifyTrackIds.joinToString(",")}",
            oauth2 = oauth2CC,
            lowPrio = true,
        )

        return apiResponseCache.getOrNull(job)
            ?.fromJson<SpotifyTrackAudioFeaturesResponse>()
            ?.audioFeatures
            ?.filterNotNull()
            ?.also { spotifyDao.insertAudioFeatures(it) }
    }

    private suspend fun getBestAlbumMatch(albumTitle: String, artists: Collection<IArtistCredit>): SpotifyAlbum? {
        val params = mutableMapOf("album" to albumTitle)
        artists.joined()?.also { params["artist"] = it }

        return search(type = "album", params = params)
            ?.albums
            ?.items
            ?.map { it.matchAlbumCombo(albumTitle, artists.names()) }
            ?.minByOrNull { it.distance }
            ?.spotifyAlbum
            ?.let { getAlbum(it.id) }
    }

    private fun getSearchQuery(params: Map<String, String?>, freeText: String? = null): String {
        return params.filterValuesNotNull()
            .map { (key, value) -> "$key:${URLEncoder.encode(value, "UTF-8")}" }
            .let { if (!freeText.isNullOrBlank()) listOf(freeText) + it else it }
            .joinToString(" ")
    }

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

    private suspend fun matchAlbumCombo(albumTitle: String, artists: Collection<IArtistCredit>): AbstractSpotifyAlbum? {
        val params = mutableMapOf("album" to albumTitle)

        artists.joined()?.also { params["artist"] = it }

        return search(type = "album", params = params)
            ?.albums
            ?.items
            ?.map { it.matchAlbumCombo(albumTitle, artists.names()) }
            ?.filter { it.distance <= 5 }
            ?.minByOrNull { it.distance }
            ?.spotifyAlbum
    }

    private fun getSearchUrl(
        type: String,
        params: Map<String, String>,
        freeText: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): String {
        val query = URLEncoder.encode(getSearchQuery(params, freeText), "UTF-8")

        return Request.getUrl(
            url = "$API_ROOT/search?q=$query&type=$type",
            params = mapOf("limit" to limit.toString(), "offset" to offset.toString()),
        )
    }

    private suspend fun matchArtist(name: String, lowPrio: Boolean = false): SpotifyArtist? =
        search(type = "artist", params = mapOf("artist" to name), lowPrio = lowPrio)
            ?.artists
            ?.items
            ?.firstOrNull { it.name.lowercase() == name.lowercase() }

    private suspend fun search(
        type: String,
        params: Map<String, String>,
        limit: Int = 20,
        offset: Int = 0,
        lowPrio: Boolean = false,
    ): SpotifySearchResponse? = searchByUrl(
        url = getSearchUrl(type = type, params = params, limit = limit, offset = offset),
        lowPrio = lowPrio,
    )

    private suspend fun searchAlbums(albumTitle: String, artistString: String?): SpotifySearchResponse? {
        val params = mutableMapOf("album" to albumTitle)

        artistString?.also { params["artist"] = it }
        return search(type = "album", params = params)
    }

    private suspend fun searchByUrl(url: String, lowPrio: Boolean = false): SpotifySearchResponse? {
        val job = RequestJob(url = url, oauth2 = oauth2CC, lowPrio = lowPrio)

        return apiResponseCache.getOrNull(job)?.fromJson<SpotifySearchResponse>()
    }

    companion object {
        // "Based on testing, we found that Spotify allows for approximately 180 requests per minute without returning
        // the error 429" -- https://community.spotify.com/t5/Spotify-for-Developers/Web-API-ratelimit/td-p/5330410
        // So let's be overly cautious.
        const val MAX_ALBUM_MATCH_DISTANCE = 1.0
        const val REQUEST_LIMIT_PER_MINUTE = 100
        const val API_ROOT = "https://api.spotify.com/v1"
    }
}
