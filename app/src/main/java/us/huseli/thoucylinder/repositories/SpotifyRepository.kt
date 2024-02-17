package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.SpotifyOAuth2
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyResponseAlbumItem
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySearchResponse
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.dataclasses.spotify.toSpotifyAlbumResponse
import us.huseli.thoucylinder.fromJson
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepository @Inject constructor(database: Database, @ApplicationContext context: Context) {
    private val albumDao = database.albumDao()
    private val apiResponseCache = MutexCache<String, String> { url ->
        if (lastMinuteRequestCount < REQUEST_LIMIT_PER_MINUTE) {
            oauth2.getToken()?.accessToken?.let { accessToken ->
                Request(url = url, headers = mapOf("Authorization" to "Bearer $accessToken"))
                    .getString()
                    .also { requestInstants.value += Instant.now() }
            }
        } else null
    }
    private val lastMinuteRequestCount: Int
        get() {
            val oneMinuteAgo = Instant.now().minusSeconds(60)
            return requestInstants.value.filter { it >= oneMinuteAgo }.size
        }
    private val requestInstants = MutableStateFlow<List<Instant>>(emptyList())

    private val _allUserAlbumsFetched = MutableStateFlow(false)
    private val _nextUserAlbumIdx = MutableStateFlow(0)
    private val _totalUserAlbumCount = MutableStateFlow<Int?>(null)
    private val _userAlbums = MutableStateFlow<List<SpotifyResponseAlbumItem.Album>>(emptyList())

    val oauth2 = SpotifyOAuth2(context)
    val allUserAlbumsFetched: StateFlow<Boolean> = _allUserAlbumsFetched.asStateFlow()
    val nextUserAlbumIdx: StateFlow<Int> = _nextUserAlbumIdx.asStateFlow()
    val totalUserAlbumCount: StateFlow<Int?> = _totalUserAlbumCount.asStateFlow()
    val userAlbums: StateFlow<List<SpotifyResponseAlbumItem.Album>> = _userAlbums.asStateFlow()
    val thumbnailCache = MutexCache<SpotifyResponseAlbumItem.Album, ImageBitmap> {
        it.images.toMediaStoreImage()?.getThumbnailImageBitmap(context)
    }

    suspend fun fetchNextUserAlbums(): Boolean {
        if (!_allUserAlbumsFetched.value) {
            val url = "${API_ROOT}/me/albums?limit=50&offset=${_nextUserAlbumIdx.value}"

            apiResponseCache.getOrNull(url, cacheNulls = false)?.toSpotifyAlbumResponse()?.also { response ->
                _userAlbums.value += response.items.map { it.album }
                _nextUserAlbumIdx.value += response.items.size
                _allUserAlbumsFetched.value = response.next == null
                _totalUserAlbumCount.value = response.total
                return true
            }
        }
        return false
    }

    suspend fun listImportedAlbumIds() = albumDao.listImportedSpotifyIds()

    suspend fun searchAlbumArt(album: Album): List<MediaStoreImage> = album.spotifyImage?.let { listOf(it) }
        ?: searchAlbums(album)
            ?.albums
            ?.items
            ?.filter { album.getLevenshteinDistance(it.toAlbum(isInLibrary = false)) < 10 }
            ?.mapNotNull { it.images.toMediaStoreImage() }
        ?: emptyList()

    private fun getSearchQuery(params: Map<String, String?>): String = params.filterValuesNotNull()
        .map { (key, value) -> "$key:${URLEncoder.encode(value, "UTF-8")}" }
        .joinToString(" ")

    private suspend fun searchAlbums(album: Album): SpotifySearchResponse? {
        val query = withContext(Dispatchers.IO) {
            URLEncoder.encode(getSearchQuery(mapOf("album" to album.title, "artist" to album.artist)), "UTF-8")
        }
        val url = "$API_ROOT/search?q=$query&type=album"

        return apiResponseCache.getOrNull(url)?.fromJson<SpotifySearchResponse>()
    }

    companion object {
        // "Based on testing, we found that Spotify allows for approximately 180 requests per minute without returning
        // the error 429" -- https://community.spotify.com/t5/Spotify-for-Developers/Web-API-ratelimit/td-p/5330410
        // So let's be overly cautious.
        const val REQUEST_LIMIT_PER_MINUTE = 50
        const val API_ROOT = "https://api.spotify.com/v1"
    }
}
