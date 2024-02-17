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
import us.huseli.thoucylinder.MutexCache
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.YoutubeTrackSearchMediator
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.YoutubePlaylistCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeImage
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadataList
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.fromJson
import us.huseli.thoucylinder.yquery
import java.io.File
import java.time.Instant
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
    ) {
        suspend fun toMediaStoreImage() =
            fullImage?.let { MediaStoreImage.fromUrls(fullImageUrl = it.url, thumbnailUrl = thumbnail?.url) }
    }

    private val _albumDownloadTasks = MutableStateFlow<List<AlbumDownloadTask>>(emptyList())
    private val _isSearchingTracks = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestCache = MutexCache<Request, String> { request -> request.getString() }
    private val responseType = object : TypeToken<Map<String, *>>() {}
    private val gson: Gson = GsonBuilder().create()

    @Suppress("UNCHECKED_CAST")
    private val metadataCache = MutexCache<String, YoutubeMetadataList> { videoId ->
        val data: Map<String, *> = mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to "ANDROID",
                    "clientVersion" to HEADER_X_YOUTUBE_CLIENT_VERSION_ANDROID,
                    "androidSdkVersion" to HEADER_ANDROID_SDK_VERSION,
                    "userAgent" to USER_AGENT_ANDROID,
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
            url = PLAYER_URL,
            headers = mapOf(
                "User-Agent" to USER_AGENT_ANDROID,
                "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME_ANDROID,
                "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION_ANDROID,
                "Origin" to "https://www.youtube.com",
            ),
            json = data,
        ).getJson()
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

    val albumDownloadTasks = _albumDownloadTasks.asStateFlow()
    val isSearchingTracks = _isSearchingTracks.asStateFlow()

    init {
        scope.launch {
            database.youtubeSearchDao().clearCache()
        }
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

    suspend fun ensureTrackMetadata(
        track: Track,
        forceReload: Boolean = false,
        onChanged: suspend (Track) -> Unit = {},
    ): Track {
        val metadataIsOld = track.youtubeVideo?.expiresAt?.isBefore(Instant.now())
        var changed: Boolean
        val youtubeMetadata =
            if (track.youtubeVideo?.metadata == null || forceReload || metadataIsOld == true)
                getBestMetadata(track, forceReload)
            else track.youtubeVideo.metadata
        changed = youtubeMetadata != track.youtubeVideo?.metadata
        val metadata =
            if (track.metadata == null || forceReload) youtubeMetadata?.toTrackMetadata()
            else track.metadata
        changed = changed || metadata != track.metadata
        val updatedTrack = track.copy(
            metadata = metadata ?: track.metadata,
            youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
        )

        if (changed) onChanged(updatedTrack)
        return updatedTrack
    }

    suspend fun getBestMetadata(track: Track, forceReload: Boolean = false): YoutubeMetadata? =
        track.youtubeVideo?.id?.let { videoId ->
            metadataCache.getOrNull(key = videoId, cacheNulls = false, forceReload = forceReload)?.getBest(
                mimeTypeFilter = VIDEO_MIMETYPE_FILTER,
                mimeTypeExclude = VIDEO_MIMETYPE_EXCLUDE,
                preferredMimetypes = VIDEO_MIMETYPE_PREFERRED,
            )
        }

    suspend fun getTrackSearchResult(query: String, continuationToken: String? = null): TrackSearchResult {
        val requestData: MutableMap<String, Any> = mutableMapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "userAgent" to USER_AGENT_ANDROID,
                    "hl" to "en",
                    "clientName" to HEADER_X_YOUTUBE_CLIENT_NAME_WEB,
                    "clientVersion" to HEADER_X_YOUTUBE_CLIENT_VERSION_WEB,
                )
            ),
            "params" to "EgIQAQ==",
        )

        if (continuationToken != null) requestData["continuation"] = continuationToken
        else requestData["query"] = query

        val response = requestCache.get(
            Request.postJson(
                url = SEARCH_URL,
                headers = mapOf(
                    "User-Agent" to USER_AGENT_ANDROID,
                    "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME_WEB,
                    "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION_WEB,
                    "Origin" to "https://www.youtube.com",
                ),
                json = requestData,
            )
        ).fromJson<Map<String, *>>()
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
                            image = imageData?.toMediaStoreImage(),
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

    suspend fun matchAlbumWithTracks(
        combo: AlbumWithTracksCombo,
        progressCallback: (Double) -> Unit = {},
    ): List<YoutubePlaylistCombo.AlbumMatch> = searchPlaylistCombos(
        combo.album.artist?.let { "${combo.album.artist} ${combo.album.title}" } ?: combo.album.title,
        progressCallback,
    ).map { it.matchAlbumWithTracks(combo) }

    suspend fun searchAlbumsWithTracks(
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<AlbumWithTracksCombo> =
        searchPlaylistCombos(query, progressCallback).map { it.toAlbumCombo(isInLibrary = false) }

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
        val response = requestCache.get(
            Request.postJson(
                url = BROWSE_URL,
                headers = mapOf(
                    "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME_ANDROID,
                    "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION_ANDROID,
                    "Origin" to "https://www.youtube.com",
                ),
                json = mutableMapOf(
                    "context" to mapOf(
                        "client" to mapOf(
                            "userAgent" to USER_AGENT_ANDROID,
                            "hl" to "en",
                            "clientName" to HEADER_X_YOUTUBE_CLIENT_NAME_WEB,
                            "clientVersion" to HEADER_X_YOUTUBE_CLIENT_VERSION_WEB,
                        )
                    ),
                    "browseId" to "VL$playlistId",
                ),
            )
        ).fromJson<Map<String, *>>()
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

    private suspend fun searchPlaylistCombos(
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<YoutubePlaylistCombo> {
        val body = requestCache.get(
            Request(
                url = "https://www.youtube.com/results",
                params = mapOf("search_query" to query),
                headers = mapOf(
                    "User-Agent" to CUSTOM_USER_AGENT,
                    "Accept" to "*/*",
                )
            )
        )
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

    companion object {
        const val DOWNLOAD_CHUNK_SIZE = 10 shl 16
        const val HEADER_ANDROID_SDK_VERSION = 30
        const val HEADER_X_YOUTUBE_CLIENT_NAME_ANDROID = "3"
        const val HEADER_X_YOUTUBE_CLIENT_NAME_WEB = "WEB"
        const val HEADER_X_YOUTUBE_CLIENT_VERSION_ANDROID = "17.31.35"
        const val HEADER_X_YOUTUBE_CLIENT_VERSION_WEB = "2.20231003.02.02"
        const val BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse"
        const val USER_AGENT_ANDROID = "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip"
        const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
        const val SEARCH_URL = "https://www.youtube.com/youtubei/v1/search"
        val VIDEO_MIMETYPE_FILTER = Regex("^audio/.*$")
        val VIDEO_MIMETYPE_EXCLUDE = null
        val VIDEO_MIMETYPE_PREFERRED = listOf("audio/opus") // most preferred first
        // val VIDEO_MIMETYPE_EXCLUDE = Regex("^audio/mp4; codecs=\"mp4a\\.40.*")
    }
}
