package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.dpToPx
import us.huseli.retaintheme.extensions.toDuration
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.CUSTOM_USER_AGENT
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.YoutubeTrackSearchMediator
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.combos.YoutubePlaylistCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.joined
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeImage
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadataList
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.getMutexCache
import us.huseli.thoucylinder.mergeWith
import us.huseli.thoucylinder.yquery
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.min

data class YoutubeClient(
    val url: String,
    val params: Map<String, String>,
    val headers: Map<String, String>,
    val json: Map<String, Any?>,
) {
    suspend fun getString() = Request(url = url, params = params, headers = headers).getString()

    suspend fun postJson() = Request.postJson(
        url = url,
        params = params,
        headers = headers,
        json = json,
    ).getJson()

    companion object {
        private const val ANDROID_CLIENT_VERSION = "18.11.34"
        private const val ANDROID_USER_AGENT = "com.google.android.youtube/18.11.34 (Linux; U; Android 10) gzip"

        fun android(
            url: String,
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            json: Map<String, Any> = emptyMap(),
        ) = YoutubeClient(
            url = url,
            params = params,
            headers = mapOf(
                "Accept" to "*/*",
                "Origin" to "https://www.youtube.com",
                "User-Agent" to ANDROID_USER_AGENT,
                "X-YouTube-Client-Name" to "3",
                "X-YouTube-Client-Version" to ANDROID_CLIENT_VERSION,
            ).plus(headers),
            json = mapOf(
                "contentCheckOk" to true,
                "context" to mapOf(
                    "client" to mapOf(
                        "androidSdkVersion" to 29,
                        "clientName" to "ANDROID",
                        "clientVersion" to ANDROID_CLIENT_VERSION,
                        "gl" to null,
                        "hl" to null,
                        "osName" to "Android",
                        "osVersion" to "10",
                        "platform" to "MOBILE",
                        "userAgent" to ANDROID_USER_AGENT,
                    ),
                ),
                "params" to "2AMBCgIQBg",
                "playbackContext" to mapOf(
                    "contentPlaybackContext" to mapOf(
                        "html5Preference" to "HTML5_PREF_WANTS",
                    ),
                ),
                "racyCheckOk" to true,
            ).mergeWith(json),
        )

        fun custom(
            url: String,
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            json: Map<String, Any> = emptyMap(),
        ) = YoutubeClient(
            url = url,
            params = params,
            headers = mapOf(
                "User-Agent" to CUSTOM_USER_AGENT,
                "Accept" to "*/*",
            ).plus(headers),
            json = json,
        )

        fun web(
            url: String,
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            json: Map<String, Any> = emptyMap(),
        ) = YoutubeClient(
            url = url,
            params = params,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; SM-G981B) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/80.0.3987.162 Mobile Safari/537.36",
                "Origin" to "https://www.youtube.com",
            ).plus(headers),
            json = mutableMapOf(
                "context" to mapOf(
                    "client" to mapOf(
                        "clientName" to "WEB",
                        "clientVersion" to "2.20231003.02.02",
                    )
                ),
                "params" to "EgIQAQ==",
            ).mergeWith(json),
        )
    }
}

@Singleton
class YoutubeRepository @Inject constructor(
    private val database: Database,
    @ApplicationContext private val context: Context,
) {
    data class VideoSearchResult(
        val videos: List<YoutubeVideo>,
        val token: String? = null,
        val nextToken: String? = null,
    ) {
        val tracks: List<Track>
            get() = videos.map { it.toTrack(isInLibrary = false) }
    }

    data class ImageData(
        val fullImage: YoutubeImage?,
        val thumbnail: YoutubeImage?,
    )

    private val _albumDownloadTasks = MutableStateFlow<List<AlbumDownloadTask>>(emptyList())
    private val _isSearchingTracks = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val responseType = object : TypeToken<Map<String, *>>() {}
    private val gson: Gson = GsonBuilder().create()
    private val youtubeSearchDao = database.youtubeSearchDao()

    @Suppress("UNCHECKED_CAST")
    private val metadataCache =
        getMutexCache<String, YoutubeMetadataList>("YoutubeRepository.metadataCache") { videoId ->
            val response = YoutubeClient.android(url = PLAYER_URL, json = mapOf("videoId" to videoId)).postJson()
            val formats =
                (response["streamingData"] as? Map<*, *>)?.get("formats") as? Collection<Map<*, *>> ?: emptyList()
            val adaptiveFormats =
                (response["streamingData"] as? Map<*, *>)?.get("adaptiveFormats") as? Collection<Map<*, *>>
                    ?: emptyList()
            val metadataList = YoutubeMetadataList()

            formats.plus(adaptiveFormats).forEach { fmt ->
                val mimeType = fmt["mimeType"] as? String
                val bitrate = fmt["bitrate"] as? Double
                val sampleRate = fmt["audioSampleRate"] as? String
                val url = fmt["url"] as? String

                if (mimeType != null && bitrate != null && sampleRate != null && url != null) {
                    metadataList.metadata.add(
                        YoutubeMetadata(
                            mimeType = mimeType,
                            bitrate = bitrate.toInt(),
                            sampleRate = sampleRate.toInt(),
                            url = url,
                            size = (fmt["contentLength"] as? String)?.toInt(),
                            channels = (fmt["audioChannels"] as? Double)?.toInt(),
                            loudnessDb = fmt["loudnessDb"] as? Double,
                            durationMs = (fmt["approxDurationMs"] as? String)?.toLong(),
                        )
                    )
                }
            }

            metadataList
        }

    val albumDownloadTasks = _albumDownloadTasks.asStateFlow()
    val isSearchingTracks = _isSearchingTracks.asStateFlow()

    init {
        scope.launch { youtubeSearchDao.clearCache() }
    }

    fun addAlbumDownloadTask(value: AlbumDownloadTask) {
        _albumDownloadTasks.value += value
    }

    suspend fun downloadVideo(url: String, tempFile: File, progressCallback: (Double) -> Unit) {
        withContext(Dispatchers.IO) {
            tempFile.outputStream().use { outputStream ->
                var rangeStart = 0
                var finished = false
                var contentLength: Int? = null

                while (!finished) {
                    val conn = Request(
                        url = url,
                        headers = mapOf("Range" to "bytes=$rangeStart-${DOWNLOAD_CHUNK_SIZE + rangeStart}"),
                    ).connect()
                    val contentRange = conn.getHeaderField("Content-Range")?.parseContentRange()

                    if (contentLength == null)
                        contentLength = contentRange?.size ?: conn.getHeaderField("Content-Length")?.toInt()
                    conn.inputStream.use { outputStream.write(it.readBytes()) }
                    if (contentLength != null) {
                        progressCallback(min((DOWNLOAD_CHUNK_SIZE + rangeStart).toDouble() / contentLength, 1.0))
                    }
                    if (contentRange?.size != null && contentRange.size - contentRange.rangeEnd > 1)
                        rangeStart = contentRange.rangeEnd + 1
                    else finished = true
                }
                progressCallback(1.0)
            }
        }
    }

    suspend fun ensureTrackMetadata(
        track: Track,
        forceReload: Boolean = false,
        onChanged: suspend (Track) -> Unit = {},
    ): Track {
        var changed: Boolean
        val youtubeMetadata =
            if (forceReload || track.youtubeVideo?.metadataRefreshNeeded == true) getBestMetadata(track, forceReload)
            else track.youtubeVideo?.metadata
        changed = youtubeMetadata != track.youtubeVideo?.metadata
        val metadata =
            if (track.metadata == null || forceReload) youtubeMetadata?.toTrackMetadata()
            else track.metadata
        changed = changed || metadata != track.metadata
        val updatedTrack = track.copy(
            metadata = metadata ?: track.metadata,
            youtubeVideo = track.youtubeVideo?.copy(
                metadata = youtubeMetadata,
                durationMs = youtubeMetadata?.durationMs ?: track.youtubeVideo.durationMs,
            ),
            durationMs = metadata?.durationMs ?: track.durationMs,
        )

        if (changed) onChanged(updatedTrack)
        return updatedTrack
    }

    suspend fun ensureTrackPlayUriOrNull(
        track: Track,
        albumArtists: List<Artist>? = null,
        albumArtist: String? = albumArtists?.joined(),
        trackArtists: List<AbstractArtistCredit>? = null,
        matchIfNeeded: Boolean = true,
        onChanged: suspend (Track) -> Unit = {},
    ): Track? {
        if (track.localUri != null) return track
        if (track.youtubeVideo != null) return ensureTrackMetadataOrNull(track, onChanged = onChanged)
        return if (matchIfNeeded)
            getBestTrackMatch(
                track = track,
                albumArtists = albumArtists,
                albumArtist = albumArtist,
                trackArtists = trackArtists,
                withMetadata = true,
            )?.also { onChanged(it) }
        else null
    }

    suspend fun getBestAlbumMatch(
        combo: AlbumWithTracksCombo,
        maxDistance: Double = 1.0,
        progressCallback: (Double) -> Unit = {},
    ): YoutubePlaylistCombo.AlbumMatch? {
        val playlistCombos = searchPlaylistCombos(
            query = combo.artists.joined()?.let { "$it ${combo.album.title}" } ?: combo.album.title,
            progressCallback = progressCallback,
        )

        return playlistCombos
            .map { it.matchAlbumWithTracks(combo) }
            .filter { it.distance <= maxDistance }
            .minByOrNull { it.distance }
    }

    suspend fun getBestMetadata(track: Track, forceReload: Boolean = false): YoutubeMetadata? =
        track.youtubeVideo?.id?.let { videoId ->
            metadataCache.getOrNull(videoId, retryOnNull = true, forceReload = forceReload)?.getBest(
                mimeTypeFilter = VIDEO_MIMETYPE_FILTER,
                mimeTypeExclude = VIDEO_MIMETYPE_EXCLUDE,
                preferredMimetypes = VIDEO_MIMETYPE_PREFERRED,
            )
        }

    suspend fun getBestTrackMatch(
        trackCombo: TrackCombo,
        maxDistance: Int = 5,
        albumArtists: List<Artist>? = null,
        withMetadata: Boolean = false,
    ): TrackCombo? = getBestTrackMatch(
        track = trackCombo.track,
        albumArtists = albumArtists,
        trackArtists = trackCombo.artists,
        albumArtist = trackCombo.albumArtist,
        maxDistance = maxDistance,
        withMetadata = withMetadata,
    )?.let { track -> trackCombo.copy(track = track) }

    suspend fun getVideoSearchResult(query: String, continuationToken: String? = null): VideoSearchResult {
        val json =
            if (continuationToken != null) mapOf("continuation" to continuationToken)
            else mapOf("query" to query)
        val response = YoutubeClient.web(url = SEARCH_URL, json = json).postJson()
        val videos = mutableListOf<YoutubeVideo>()
        var nextContinuationToken: String? = null

        val sectionContents =
            if (continuationToken == null) response.yquery<Collection<Map<*, *>>>(
                "contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents"
            )
            else response.yquery<Collection<Map<*, *>>>("onResponseReceivedCommands")
                ?.firstOrNull()?.yquery<Collection<Map<*, *>>>("appendContinuationItemsAction.continuationItems")

        sectionContents?.forEach { sectionContent ->
            sectionContent.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents")?.forEach { itemContent ->
                val videoId = itemContent.yquery<String>("videoRenderer.videoId")
                val title = itemContent.yquery<Collection<Map<*, *>>>("videoRenderer.title.runs")
                    ?.firstOrNull()?.get("text") as? String
                val imageData = getImageData(
                    listOf(itemContent.yquery<Collection<Map<*, *>>>("videoRenderer.thumbnail.thumbnails"))
                )
                val lengthText = itemContent.yquery<String>("videoRenderer.lengthText.simpleText")
                val durationMs = lengthText?.toDuration()?.inWholeMilliseconds

                if (videoId != null && title != null) {
                    videos.add(
                        YoutubeVideo(
                            id = videoId,
                            title = title,
                            thumbnail = imageData?.thumbnail,
                            fullImage = imageData?.fullImage,
                            durationMs = durationMs,
                        )
                    )
                }
            }

            sectionContent.yquery<String>(
                "continuationItemRenderer.continuationEndpoint.continuationCommand.token"
            )?.also { nextContinuationToken = it }
        }

        return VideoSearchResult(
            videos = videos,
            token = continuationToken,
            nextToken = nextContinuationToken,
        )
    }

    suspend fun searchPlaylistCombos(
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<YoutubePlaylistCombo> {
        val body = YoutubeClient.custom(
            url = "https://www.youtube.com/results",
            params = mapOf("search_query" to query),
        ).getString()
        val ytDataJson = Regex("var ytInitialData *= *(\\{.*?\\});", RegexOption.MULTILINE)
            .find(body)
            ?.groupValues
            ?.lastOrNull()
        val ytData = ytDataJson?.let { gson.fromJson(it, responseType) }
        val playlists = mutableListOf<YoutubePlaylist>()

        if (ytData != null) {
            val secondaryContents = ytData.yquery<Collection<Map<*, *>>>(
                "contents.twoColumnSearchResultsRenderer.secondaryContents.secondarySearchContainerRenderer.contents"
            ) ?: emptyList()
            val headers = secondaryContents
                .mapNotNull { it.yquery<Map<*, *>>("universalWatchCardRenderer.header.watchCardRichHeaderRenderer") }

            headers.forEach { header ->
                val listTitle = header.yquery<String>("title.simpleText")
                val artist = header.yquery<String>("subtitle.simpleText")
                    ?.takeIf { it.contains("Album • ") }
                    ?.substringAfter("Album • ")
                val playlistId =
                    header.yquery<String>("titleNavigationEndpoint.commandMetadata.webCommandMetadata.url")
                        ?.split("=")
                        ?.last()

                if (listTitle != null && playlistId != null) {
                    val title =
                        artist?.let { listTitle.replace(Regex(Pattern.quote("^$it - ")), "") } ?: listTitle

                    playlists.add(YoutubePlaylist(artist = artist, title = title, id = playlistId))
                }
            }

            val primaryContents = ytData.yquery<Collection<Map<*, *>>>(
                "contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents"
            ) ?: emptyList()
            val sections = primaryContents
                .mapNotNull {
                    it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents")
                        ?.filter { section -> section.containsKey("playlistRenderer") }
                }
                .flatten()

            sections.forEach { section ->
                val playlistId = section.yquery<String>("playlistRenderer.playlistId")
                val listTitle = section.yquery<String>("playlistRenderer.title.simpleText")

                if (
                    listTitle != null
                    && playlistId != null
                    && !playlists.map { it.id }.contains(playlistId)
                ) playlists.add(YoutubePlaylist(title = listTitle, id = playlistId))
            }

            val progressIncrement = 1.0 / (playlists.size + 1)

            progressCallback(progressIncrement)

            return playlists.mapIndexedNotNull { index, playlist ->
                getPlaylistComboFromPlaylist(playlist.id, playlist.artist).also {
                    progressCallback(progressIncrement * (index + 2))
                }
            }
        }
        return emptyList()
    }

    @OptIn(ExperimentalPagingApi::class)
    fun searchTracks(query: String): Pager<Int, Track> {
        _isSearchingTracks.value = true
        return Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 10),
            remoteMediator = YoutubeTrackSearchMediator(query, this, database),
            pagingSourceFactory = {
                youtubeSearchDao.pageTracksByQuery(query).also {
                    _isSearchingTracks.value = false
                }
            },
        )
    }


    /** PRIVATE METHODS ******************************************************/
    private suspend fun ensureTrackMetadataOrNull(
        track: Track,
        forceReload: Boolean = false,
        onChanged: suspend (Track) -> Unit = {},
    ): Track? = ensureTrackMetadata(track = track, forceReload = forceReload, onChanged = onChanged)
        .takeIf { it.metadata != null && it.youtubeVideo?.metadata != null }

    private suspend fun getBestTrackMatch(
        track: Track,
        albumArtists: List<Artist>? = null,
        albumArtist: String? = albumArtists?.joined(),
        trackArtists: List<AbstractArtistCredit>? = null,
        maxDistance: Int = 5,
        withMetadata: Boolean = false,
    ): Track? {
        val artistString = trackArtists?.joined() ?: albumArtist
        val query = artistString?.let { "$it ${track.title}" } ?: track.title
        val videos = getVideoSearchResult(query).videos

        return videos
            .map {
                it.matchTrack(
                    track = track,
                    albumArtists = albumArtists,
                    albumArtist = albumArtist,
                    trackArtists = trackArtists,
                )
            }
            .filter { it.distance <= maxDistance }
            .minByOrNull { it.distance }
            ?.let { match ->
                track.copy(youtubeVideo = match.video).let { if (withMetadata) ensureTrackMetadataOrNull(it) else it }
            }
    }

    private fun getImageData(thumbnailSections: List<Collection<Map<*, *>>?>): ImageData? {
        /**
         * Extracts image URLs from API response, works for both playlists and videos.
         * Does not fetch the actual images.
         * Used by getTrackSearchResult() & listPlaylistDetails().
         */
        val thumbnailSizePx = context.dpToPx(IMAGE_MAX_DP_THUMBNAIL * IMAGE_MAX_DP_THUMBNAIL)
        val images = mutableListOf<YoutubeImage>()

        thumbnailSections.forEach { section ->
            section?.forEach { collection ->
                val tnUrl = collection["url"] as? String
                val tnWidth = collection["width"] as? Double
                val tnHeight = collection["height"] as? Double
                if (tnUrl != null && tnWidth != null && tnHeight != null) {
                    images.add(YoutubeImage(url = tnUrl, width = tnWidth.toInt(), height = tnHeight.toInt()))
                }
            }
        }

        return if (images.isNotEmpty()) ImageData(
            thumbnail = images
                .sortedBy { (it.size - thumbnailSizePx).absoluteValue }
                .minByOrNull { it.size < thumbnailSizePx },
            fullImage = images.maxByOrNull { it.size },
        )
        else null
    }

    private suspend fun getPlaylistComboFromPlaylist(
        playlistId: String,
        artist: String? = null,
    ): YoutubePlaylistCombo? {
        val response = YoutubeClient.web(
            url = BROWSE_URL,
            json = mapOf("browseId" to "VL$playlistId"),
        ).postJson()
        val title = response.yquery<String>("metadata.playlistMetadataRenderer.albumName")
            ?: response.yquery<String>("header.playlistHeaderRenderer.title.simpleText")
        val videoCount = response.yquery<Collection<Map<*, *>>>("header.playlistHeaderRenderer.numVideosText.runs")
            ?.firstOrNull()?.get("text") as? String
        val videoArtist = response.yquery<String>("header.playlistHeaderRenderer.subtitle.simpleText")
            ?.substringBeforeLast(" • Album")

        if (title != null) {
            val thumbnailRenderer = response.yquery<Collection<Map<*, *>>>("sidebar.playlistSidebarRenderer.items")
                ?.firstOrNull()
                ?.yquery<Map<*, *>>("playlistSidebarPrimaryInfoRenderer.thumbnailRenderer")
            val imageData = getImageData(
                listOf(
                    thumbnailRenderer?.yquery<Collection<Map<*, *>>>("playlistCustomThumbnailRenderer.thumbnail.thumbnails"),
                    thumbnailRenderer?.yquery<Collection<Map<*, *>>>("playlistVideoThumbnailRenderer.thumbnail.thumbnails"),
                )
            )
            val videos = mutableListOf<YoutubeVideo>()
            val playlist = YoutubePlaylist(
                id = playlistId,
                title = title,
                artist = artist ?: videoArtist,
                videoCount = videoCount?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0,
                thumbnail = imageData?.thumbnail,
                fullImage = imageData?.fullImage,
            )

            response.yquery<Collection<Map<*, *>>>("contents.twoColumnBrowseResultsRenderer.tabs")
                ?.firstOrNull()
                ?.yquery<Collection<Map<*, *>>>("tabRenderer.content.sectionListRenderer.contents")
                ?.firstOrNull()
                ?.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents")
                ?.firstOrNull()
                ?.yquery<Collection<Map<*, *>>>("playlistVideoListRenderer.contents")
                ?.forEach { video ->
                    val videoId = video.yquery<String>("playlistVideoRenderer.videoId")
                    val videoTitle = video.yquery<Collection<Map<*, *>>>("playlistVideoRenderer.title.runs")
                        ?.firstOrNull()?.get("text") as? String
                    val lengthSeconds = video.yquery<String>("playlistVideoRenderer.lengthSeconds")

                    if (videoId != null && videoTitle != null) {
                        videos.add(
                            YoutubeVideo(
                                id = videoId,
                                title = videoTitle,
                                durationMs = lengthSeconds?.toLong()?.times(1000),
                            )
                        )
                    }
                }

            return YoutubePlaylistCombo(playlist = playlist, videos = videos)
        }
        return null
    }

    companion object {
        const val DOWNLOAD_CHUNK_SIZE = 10 shl 16
        const val BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse"
        const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
        const val SEARCH_URL = "https://www.youtube.com/youtubei/v1/search"
        val VIDEO_MIMETYPE_FILTER = Regex("^audio/.*$")
        val VIDEO_MIMETYPE_EXCLUDE = null
        val VIDEO_MIMETYPE_PREFERRED = listOf("audio/opus") // most preferred first
        // val VIDEO_MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
    }
}
