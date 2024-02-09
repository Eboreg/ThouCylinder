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
import us.huseli.thoucylinder.Constants.DOWNLOAD_CHUNK_SIZE
import us.huseli.thoucylinder.Constants.HEADER_ANDROID_SDK_VERSION
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_NAME
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_VERSION
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_EXCLUDE
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_FILTER
import us.huseli.thoucylinder.Constants.YOUTUBE_BROWSE_URL
import us.huseli.thoucylinder.Constants.YOUTUBE_USER_AGENT
import us.huseli.thoucylinder.Constants.YOUTUBE_PLAYER_URL
import us.huseli.thoucylinder.Constants.YOUTUBE_SEARCH_URL
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.YoutubeTrackSearchMediator
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeMetadataList
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubeImage
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.getJson
import us.huseli.thoucylinder.getString
import us.huseli.thoucylinder.yquery
import java.io.File
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.min

@Singleton
class YoutubeRepository @Inject constructor(
    private val database: Database,
    @ApplicationContext private val context: Context,
) {
    data class TrackSearchResult(
        val tracks: List<Track>,
        val token: String? = null,
        val nextToken: String? = null,
    )

    data class ImageData(
        val fullImage: YoutubeImage?,
        val thumbnail: YoutubeImage?,
    )

    private val _albumDownloadTasks = MutableStateFlow<List<AlbumDownloadTask>>(emptyList())
    private val _isSearchingTracks = MutableStateFlow(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Suppress("UNCHECKED_CAST")
    private val metadataCache = MutexCache<String, YoutubeMetadataList> { videoId ->
        val data: Map<String, *> = mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to "ANDROID",
                    "clientVersion" to HEADER_X_YOUTUBE_CLIENT_VERSION,
                    "androidSdkVersion" to HEADER_ANDROID_SDK_VERSION,
                    "userAgent" to YOUTUBE_USER_AGENT,
                    "hl" to "en",
                    "timeZone" to "UTC",
                    "utcOffsetMinutes" to 0,
                ),
            ),
            "playbackContext" to mapOf(
                "contentPlaybackContext" to mapOf(
                    "html5Preference" to "HTML5_PREF_WANTS",
                ),
            ),
            "params" to "CgIQBg==",
            "videoId" to videoId,
            "contentCheckOk" to true,
            "racyCheckOk" to true,
        )
        val response = Request.postJson(
            url = YOUTUBE_PLAYER_URL,
            headers = mapOf(
                "User-Agent" to YOUTUBE_USER_AGENT,
                "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME,
                "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION,
                "Origin" to "https://www.youtube.com",
            ),
            json = data,
        ).connect().getJson()
        val formats =
            (response["streamingData"] as? Map<*, *>)?.get("formats") as? Collection<Map<*, *>> ?: emptyList()
        val adaptiveFormats =
            (response["streamingData"] as? Map<*, *>)?.get("adaptiveFormats") as? Collection<Map<*, *>> ?: emptyList()
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

    val gson: Gson = GsonBuilder().create()
    val albumDownloadTasks = _albumDownloadTasks.asStateFlow()
    val responseType = object : TypeToken<Map<String, *>>() {}
    val isSearchingTracks = _isSearchingTracks.asStateFlow()

    init {
        scope.launch {
            database.youtubeSearchDao().clearCache()
        }
    }

    fun addAlbumDownloadTask(value: AlbumDownloadTask) {
        _albumDownloadTasks.value += value
    }

    suspend inline fun downloadVideo(url: String, tempFile: File, crossinline progressCallback: (Double) -> Unit) {
        withContext(Dispatchers.IO) {
            tempFile.outputStream().use { outputStream ->
                var rangeStart = 0
                var finished = false
                var contentLength: Int? = null

                while (!finished) {
                    val conn = Request.get(
                        url = url,
                        headers = mapOf("Range" to "bytes=$rangeStart-${DOWNLOAD_CHUNK_SIZE + rangeStart}"),
                    ).connect()
                    val contentRange = conn.getHeaderField("Content-Range")?.parseContentRange()

                    if (contentLength == null)
                        contentLength = contentRange?.size ?: conn.getHeaderField("Content-Length")?.toInt()
                    conn.getInputStream().use { outputStream.write(it.readBytes()) }
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

    suspend inline fun getAlbumSearchResult(
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<AlbumWithTracksCombo> {
        val scrapedAlbums = mutableListOf<Album>()
        val encodedQuery = withContext(Dispatchers.IO) { URLEncoder.encode(query, "UTF-8") }
        val body = Request.get(
            url = "https://www.youtube.com/results?search_query=$encodedQuery",
            headers = mapOf(
                "User-Agent" to CUSTOM_USER_AGENT,
                "Accept" to "*/*",
            )
        ).connect().getString()
        val ytDataJson = Regex("var ytInitialData *= *(\\{.*?\\});", RegexOption.MULTILINE)
            .find(body)
            ?.groupValues
            ?.lastOrNull()
        val ytData = ytDataJson?.let { gson.fromJson(it, responseType) }

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

                    scrapedAlbums.add(
                        Album(
                            artist = artist,
                            title = title,
                            isInLibrary = false,
                            isLocal = false,
                            youtubePlaylist = YoutubePlaylist(artist = artist, title = title, id = playlistId),
                        )
                    )
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
                    && !scrapedAlbums.mapNotNull { it.youtubePlaylist?.id }.contains(playlistId)
                ) {
                    scrapedAlbums.add(
                        Album(
                            title = listTitle,
                            isInLibrary = false,
                            isLocal = false,
                            youtubePlaylist = YoutubePlaylist(title = listTitle, id = playlistId),
                        )
                    )
                }
            }

            val progressIncrement = 1.0 / (scrapedAlbums.size + 1)

            progressCallback(progressIncrement)

            return scrapedAlbums.mapIndexedNotNull { index, scrapedAlbum ->
                scrapedAlbum.youtubePlaylist?.id?.let { playlistId ->
                    getAlbumWithTracksFromPlaylist(playlistId, scrapedAlbum.artist).also {
                        progressCallback(progressIncrement * (index + 2))
                    }
                }
            }
        }

        return emptyList()
    }

    suspend fun getAlbumWithTracksFromPlaylist(playlistId: String, artist: String?): AlbumWithTracksCombo? {
        val requestData: MutableMap<String, Any> = mutableMapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "userAgent" to YOUTUBE_USER_AGENT,
                    "hl" to "en",
                    "clientName" to "WEB",
                    "clientVersion" to "2.20231003.02.02",
                )
            ),
            "browseId" to "VL$playlistId",
        )
        val response = Request.postJson(
            url = YOUTUBE_BROWSE_URL,
            headers = mapOf(
                "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME,
                "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION,
                "Origin" to "https://www.youtube.com",
            ),
            json = requestData,
        ).connect().getJson()
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
            val tracks = mutableListOf<Track>()
            val album = Album(
                title = title,
                isInLibrary = false,
                isLocal = false,
                artist = artist ?: videoArtist,
                youtubePlaylist = YoutubePlaylist(
                    id = playlistId,
                    title = title,
                    artist = artist ?: videoArtist,
                    videoCount = videoCount?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0,
                    thumbnail = imageData?.thumbnail,
                    fullImage = imageData?.fullImage,
                ),
                albumArt = imageData?.fullImage?.let { MediaStoreImage.fromUrls(it.url, imageData.thumbnail?.url) },
            )

            response.yquery<Collection<Map<*, *>>>("contents.twoColumnBrowseResultsRenderer.tabs")
                ?.firstOrNull()
                ?.yquery<Collection<Map<*, *>>>("tabRenderer.content.sectionListRenderer.contents")
                ?.firstOrNull()
                ?.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents")
                ?.firstOrNull()
                ?.yquery<Collection<Map<*, *>>>("playlistVideoListRenderer.contents")
                ?.forEachIndexed { index, video ->
                    val videoId = video.yquery<String>("playlistVideoRenderer.videoId")
                    val videoTitle = video.yquery<Collection<Map<*, *>>>("playlistVideoRenderer.title.runs")
                        ?.firstOrNull()?.get("text") as? String
                    val lengthSeconds = video.yquery<String>("playlistVideoRenderer.lengthSeconds")

                    if (videoId != null && videoTitle != null) {
                        tracks.add(
                            Track(
                                isInLibrary = false,
                                albumId = album.albumId,
                                albumPosition = index + 1,
                                title = videoTitle,
                                youtubeVideo = YoutubeVideo(
                                    id = videoId,
                                    title = videoTitle,
                                    durationMs = lengthSeconds?.toLong()?.times(1000),
                                ),
                            )
                        )
                    }
                }

            return AlbumWithTracksCombo(album = album, tracks = tracks)
        }

        return null
    }

    suspend fun getBestMetadata(track: Track, forceReload: Boolean = false): YoutubeMetadata? =
        track.youtubeVideo?.id?.let { videoId ->
            getMetadataList(videoId, forceReload).getBest(
                mimeTypeFilter = VIDEO_MIMETYPE_FILTER,
                mimeTypeExclude = VIDEO_MIMETYPE_EXCLUDE,
            )
        }

    suspend fun getTrackSearchResult(query: String, continuationToken: String? = null): TrackSearchResult {
        val requestData: MutableMap<String, Any> = mutableMapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "userAgent" to YOUTUBE_USER_AGENT,
                    "hl" to "en",
                    "clientName" to "WEB",
                    "clientVersion" to "2.20231003.02.02",
                )
            ),
            "params" to "EgIQAQ==",
        )

        if (continuationToken != null) requestData["continuation"] = continuationToken
        else requestData["query"] = query

        val response = Request.postJson(
            url = YOUTUBE_SEARCH_URL,
            headers = mapOf(
                "User-Agent" to YOUTUBE_USER_AGENT,
                "X-YouTube-Client-Name" to "WEB",
                "X-YouTube-Client-Version" to "2.20231003.02.02",
                "Origin" to "https://www.youtube.com",
            ),
            json = requestData,
        ).connect().getJson()
        val tracks = mutableListOf<Track>()
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

                if (videoId != null && title != null) {
                    tracks.add(
                        Track(
                            title = title,
                            isInLibrary = false,
                            youtubeVideo = YoutubeVideo(
                                id = videoId,
                                title = title,
                                thumbnail = imageData?.thumbnail,
                                fullImage = imageData?.fullImage,
                                durationMs = lengthText?.toDuration()?.inWholeMilliseconds,
                            ),
                        )
                    )
                }
            }

            sectionContent.yquery<String>(
                "continuationItemRenderer.continuationEndpoint.continuationCommand.token"
            )?.also { nextContinuationToken = it }
        }

        return TrackSearchResult(
            tracks = tracks,
            token = continuationToken,
            nextToken = nextContinuationToken,
        )
    }

    @OptIn(ExperimentalPagingApi::class)
    fun searchTracks(query: String): Pager<Int, Track> {
        _isSearchingTracks.value = true
        return Pager(
            config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 10),
            remoteMediator = YoutubeTrackSearchMediator(query, this, database),
            pagingSourceFactory = {
                database.youtubeSearchDao().pageTracksByQuery(query).also {
                    _isSearchingTracks.value = false
                }
            },
        )
    }

    /** PRIVATE METHODS ******************************************************/

    private suspend fun getMetadataList(videoId: String, forceReload: Boolean = false): YoutubeMetadataList =
        metadataCache.get(videoId, forceReload) ?: YoutubeMetadataList()

    /**
     * Extracts image URLs from API response, works for both playlists and videos.
     * Does not fetch the actual images.
     * Used by getTrackSearchResult() & listPlaylistDetails().
     */
    private fun getImageData(thumbnailSections: List<Collection<Map<*, *>>?>): ImageData? {
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
}
