package us.huseli.thoucylinder.repositories

import android.content.Context
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.zipBy
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.DOWNLOAD_CHUNK_SIZE
import us.huseli.thoucylinder.Constants.HEADER_ANDROID_SDK_VERSION
import us.huseli.thoucylinder.Constants.HEADER_USER_AGENT
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_NAME
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_VERSION
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_EXCLUDE
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_FILTER
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.YoutubeTrackSearchMediator
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeMetadataList
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubePlaylistItem
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.TempTrackData
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.thumbnailDir
import us.huseli.thoucylinder.urlRequest
import us.huseli.thoucylinder.yquery
import java.io.File
import java.io.OutputStream
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class YoutubeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: Database,
) {
    data class TrackSearchResult(
        val tracks: List<Track>,
        val token: String? = null,
        val nextToken: String? = null,
    )

    private val _isSearchingTracks = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson: Gson = GsonBuilder().create()
    private val metadataCache = mutableMapOf<String, YoutubeMetadataList>()
    private val playlistVideoCache = mutableMapOf<String, List<YoutubeVideo>>()
    private val responseType = object : TypeToken<Map<String, *>>() {}

    val isSearchingTracks = _isSearchingTracks.asStateFlow()

    init {
        scope.launch {
            database.youtubeSearchDao().clearCache()
        }
    }

    suspend fun downloadTracks(tracks: List<Track>, progressCallback: (DownloadProgress) -> Unit): List<Track> {
        val updatedTracks = mutableListOf<Track>()

        try {
            // downloadVideo() also gets metadata if missing, but doesn't save it on the YoutubeVideo object, which
            // ensureVideoMetadata() does:
            ensureVideoMetadata(tracks).forEachIndexed { index, track ->
                if (track.youtubeVideo != null) {
                    val file = downloadVideo(video = track.youtubeVideo) {
                        progressCallback(it.copy(progress = (index + it.progress) / tracks.size))
                    }
                    updatedTracks.add(track.copy(tempTrackData = TempTrackData(localFile = file)))
                } else updatedTracks.add(track)
            }
        } catch (e: CancellationException) {
            updatedTracks.mapNotNull { it.tempTrackData?.localFile }.forEach { it.delete() }
            throw e
        }
        return updatedTracks
    }

    /**
     * @throws TrackDownloadException if downloadVideo() fails
     */
    suspend fun downloadTrack(track: Track, progressCallback: (DownloadProgress) -> Unit): Track {
        return track.youtubeVideo?.let { youtubeVideo ->
            track.copy(
                tempTrackData = TempTrackData(
                    localFile = downloadVideo(video = youtubeVideo, progressCallback = progressCallback),
                )
            )
        } ?: track
    }

    suspend fun getBestMetadata(track: Track): YoutubeMetadata? =
        track.youtubeVideo?.id?.let { getBestMetadata(it) }

    suspend fun getTrackSearchResult(query: String, continuationToken: String? = null): TrackSearchResult {
        val gson: Gson = GsonBuilder().create()
        val requestData: MutableMap<String, Any> = mutableMapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "userAgent" to HEADER_USER_AGENT,
                    "hl" to "en",
                    "clientName" to "WEB",
                    "clientVersion" to "2.20231003.02.02",
                )
            ),
            "params" to "EgIQAQ==",
        )
        if (continuationToken != null) requestData["continuation"] = continuationToken
        else requestData["query"] = query
        val requestBody = gson.toJson(requestData)
        val body = withContext(Dispatchers.IO) {
            val conn = urlRequest(
                urlString = "https://www.youtube.com/youtubei/v1/search",
                body = requestBody.toByteArray(Charsets.UTF_8),
                headers = mapOf(
                    "User-Agent" to HEADER_USER_AGENT,
                    "content-type" to "application/json",
                    "X-YouTube-Client-Name" to "WEB",
                    "X-YouTube-Client-Version" to "2.20231003.02.02",
                    "Origin" to "https://www.youtube.com",
                ),
            )
            conn.getInputStream().use { it.bufferedReader().readText() }
        }
        val response = gson.fromJson(body, responseType)
        val tracks = mutableListOf<Track>()
        var nextContinuationToken: String? = null

        if (response != null) {
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
                    val thumbnail = itemContent.yquery<Collection<Map<*, *>>>("videoRenderer.thumbnail.thumbnails")
                        ?.let { getThumbnail(it) }
                    val length = itemContent.yquery<String>("videoRenderer.lengthText.simpleText")

                    if (videoId != null && title != null) {
                        tracks.add(
                            Track(
                                title = title,
                                isInLibrary = false,
                                image = thumbnail,
                                youtubeVideo = YoutubeVideo(
                                    id = videoId,
                                    title = title,
                                    length = length,
                                )
                            )
                        )
                    }
                }

                sectionContent.yquery<String>(
                    "continuationItemRenderer.continuationEndpoint.continuationCommand.token"
                )?.also { nextContinuationToken = it }
            }
        }
        return TrackSearchResult(
            tracks = tracks,
            token = continuationToken,
            nextToken = nextContinuationToken,
        )
    }

    suspend fun populateAlbumTracks(album: Album, withMetadata: Boolean): AlbumWithTracksPojo {
        return album.youtubePlaylist?.let { playlist ->
            val videos = listPlaylistVideos(playlist = playlist, withMetadata = withMetadata)
            AlbumWithTracksPojo(
                album = album,
                tracks = videos.map { video ->
                    video.toTrack(
                        isInLibrary = false,
                        albumId = album.albumId,
                        image = video.thumbnail ?: album.albumArt,
                    )
                }
            )
        } ?: AlbumWithTracksPojo(album = album)
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

    /**
     * Used both for downloading individual tracks/videos and albums/playlists.
     * @throws TrackDownloadException if download fails or no stream URL could be found
     */
    private suspend fun downloadVideo(video: YoutubeVideo, progressCallback: (DownloadProgress) -> Unit): File {
        val metadata =
            getBestMetadata(video.id) ?: throw TrackDownloadException(TrackDownloadException.ErrorType.NO_METADATA)
        val tempFile = File(context.cacheDir, "${video.id}.${metadata.fileExtension}")
        val downloadProgress = DownloadProgress(
            item = video.title,
            status = DownloadProgress.Status.DOWNLOADING,
            progress = 0.0,
        )

        progressCallback(downloadProgress)

        try {
            reallyDownloadTrack(
                url = metadata.url,
                outputStream = tempFile.outputStream(),
                progressCallback = {
                    progressCallback(downloadProgress.copy(progress = it))
                },
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw TrackDownloadException(TrackDownloadException.ErrorType.DOWNLOAD, cause = e)
        }

        return tempFile
    }

    /** Gets & sets youtubeVideo.metadata for those tracks that have youtubeVideo but not metadata. */
    private suspend fun ensureVideoMetadata(tracks: List<Track>): List<Track> {
        return tracks.map { track ->
            if (track.youtubeVideo != null && track.youtubeVideo.metadata == null)
                track.copy(youtubeVideo = track.youtubeVideo.copy(metadata = getBestMetadata(track.youtubeVideo.id)))
            else track
        }
    }

    suspend fun getAlbumSearchResult(query: String): List<AlbumPojo> {
        val scrapedPojos = mutableListOf<AlbumPojo>()
        val encodedQuery = withContext(Dispatchers.IO) { URLEncoder.encode(query, "UTF-8") }
        val conn = urlRequest(
            urlString = "https://www.youtube.com/results?search_query=$encodedQuery",
            headers = mapOf(
                "User-Agent" to "kuken/1.2.3",
                "Accept" to "*/*",
            )
        )
        val body = withContext(Dispatchers.IO) { conn.getInputStream().use { it.bufferedReader().readText() } }
        val ytDataJson = Regex("var ytInitialData *= *(\\{.*?\\});", RegexOption.MULTILINE)
            .find(body)
            ?.groupValues
            ?.lastOrNull()
        val ytData = ytDataJson?.let { gson.fromJson(it, responseType) }

        if (ytData != null) {
            ytData.yquery<Collection<Map<*, *>>>(
                "contents.twoColumnSearchResultsRenderer.secondaryContents.secondarySearchContainerRenderer.contents"
            )?.forEach { content ->
                content.yquery<Map<*, *>>("universalWatchCardRenderer.header.watchCardRichHeaderRenderer")
                    ?.let { header ->
                        val listTitle = header.yquery<String>("title.simpleText")
                        val artist = header.yquery<String>("subtitle.simpleText")
                            ?.takeIf { it.contains("Album • ") }
                            ?.substringAfter("Album • ")
                        val playlistId =
                            header.yquery<String>("titleNavigationEndpoint.commandMetadata.webCommandMetadata.url")
                                ?.split("=")
                                ?.last()

                        if (listTitle != null && playlistId != null) {
                            val title = artist?.let { listTitle.replace(Regex("^$it - "), "") } ?: listTitle

                            scrapedPojos.add(
                                AlbumPojo(
                                    album = Album(
                                        artist = artist,
                                        title = title,
                                        isInLibrary = false,
                                        isLocal = false,
                                        youtubePlaylist = YoutubePlaylist(
                                            artist = artist,
                                            title = title,
                                            id = playlistId,
                                        ),
                                    ),
                                )
                            )
                        }
                    }
            }

            ytData.yquery<Collection<Map<*, *>>>(
                "contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents"
            )?.forEach { content ->
                content.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents")?.forEach { section ->
                    if (section.containsKey("playlistRenderer")) {
                        val playlistId = section.yquery<String>("playlistRenderer.playlistId")
                        val listTitle = section.yquery<String>("playlistRenderer.title.simpleText")
                        val videoCount = section.yquery<String>("playlistRenderer.videoCount")

                        if (
                            listTitle != null
                            && playlistId != null
                            && !scrapedPojos.mapNotNull { it.album.youtubePlaylist?.id }.contains(playlistId)
                        ) {
                            scrapedPojos.add(
                                AlbumPojo(
                                    album = Album(
                                        title = listTitle,
                                        isInLibrary = false,
                                        isLocal = false,
                                        youtubePlaylist = YoutubePlaylist(
                                            title = listTitle,
                                            id = playlistId,
                                        ),
                                    ),
                                    trackCount = videoCount?.toInt(),
                                )
                            )
                        }
                    }
                }
            }

            // We need to get playlists using both methods (this and via the regular API) because this one is the only
            // one that gives us "subtitle" (for secondaryContents), which may contain artist name, and the API gives
            // us thumbnails (of reasonable sizes) and a video count. (Also, search via API costs a bunch of credits.)
            val apiPojos = listPlaylistDetails(scrapedPojos.mapNotNull { it.album.youtubePlaylist?.id })
            return scrapedPojos
                .zipBy(apiPojos) { a, b -> a.album.youtubePlaylist?.id == b.album.youtubePlaylist?.id }
                .map { (scrapedPojo, apiPojo) ->
                    scrapedPojo.copy(
                        album = scrapedPojo.album.copy(
                            albumArt = apiPojo.album.albumArt,
                            youtubePlaylist = apiPojo.album.youtubePlaylist,
                        ),
                        trackCount = apiPojo.trackCount ?: scrapedPojo.trackCount,
                    )
                }
        }
        return emptyList()
    }

    private suspend fun getBestMetadata(videoId: String): YoutubeMetadata? =
        try {
            getMetadataList(videoId).getBest(
                mimeTypeFilter = VIDEO_MIMETYPE_FILTER,
                mimeTypeExclude = VIDEO_MIMETYPE_EXCLUDE,
            )
        } catch (e: Exception) {
            Log.e("YoutubeRepository", "getBestMetadata: $e", e)
            null
        }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getMetadataList(videoId: String): YoutubeMetadataList {
        metadataCache[videoId]?.let { return it }

        val gson: Gson = GsonBuilder().create()
        val data: Map<String, *> = mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to "ANDROID",
                    "clientVersion" to HEADER_X_YOUTUBE_CLIENT_VERSION,
                    "androidSdkVersion" to HEADER_ANDROID_SDK_VERSION,
                    "userAgent" to HEADER_USER_AGENT,
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
        val requestBody = gson.toJson(data)
        val conn = urlRequest(
            urlString = "https://www.youtube.com/youtubei/v1/player",
            headers = mapOf(
                "User-Agent" to HEADER_USER_AGENT,
                "content-type" to "application/json",
                "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME,
                "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION,
                "Origin" to "https://www.youtube.com",
            ),
            body = requestBody.toByteArray(Charsets.UTF_8),
        )
        val body = withContext(Dispatchers.IO) { conn.getInputStream().use { it.bufferedReader().readText() } }
        val response = gson.fromJson(body, responseType)
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
        metadataCache[videoId] = metadataList
        return metadataList
    }

    /**
     * Extracts thumbnail URL from API response, works for both playlists and videos.
     * Does not fetch the actual image.
     */
    private fun getThumbnail(thumbnailSections: Collection<Map<*, *>>): Image? {
        var best: Image? = null

        thumbnailSections.forEach { section ->
            val tnUrl = section["url"] as? String
            val tnWidth = section["width"] as? Double
            val tnHeight = section["height"] as? Double
            if (tnUrl != null && tnWidth != null && tnHeight != null) {
                val tn = Image(
                    url = tnUrl,
                    width = tnWidth.toInt(),
                    height = tnHeight.toInt(),
                    localFile = File(context.thumbnailDir, UUID.randomUUID().toString()),
                )
                if (best == null || best!!.size < tn.size) best = tn
            }
        }
        return best
    }

    /**
     * Fetches playlist details (including video counts but _not_ video details) from API.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun listPlaylistDetails(playlistIds: Collection<String>): List<AlbumPojo> {
        val pojos = mutableListOf<AlbumPojo>()

        if (playlistIds.isEmpty()) return pojos

        playlistIds.toSet().chunked(50).forEach { chunk ->
            val urlString = "https://youtube.googleapis.com/youtube/v3/playlists?part=snippet" +
                "&part=contentDetails&maxResults=50" +
                "&id=${chunk.joinToString(",")}&key=${BuildConfig.youtubeApiKey}"
            var finished = false
            var pageToken: String? = null

            while (!finished) {
                val conn = urlRequest(urlString + (pageToken?.let { "&pageToken=$it" } ?: ""))
                val body = conn.getInputStream().use { it.bufferedReader().readText() }
                val response = gson.fromJson(body, responseType)
                val items = response["items"] as? Collection<Map<*, *>>

                pageToken = response["nextPageToken"] as? String
                finished = pageToken == null
                items?.forEach { item ->
                    val playlistId = item["id"] as? String
                    val snippet = item["snippet"] as? Map<*, *>
                    val title = snippet?.get("title") as? String
                    val videoCount = item.yquery<Double>("contentDetails.itemCount")
                    val thumbnails = snippet?.get("thumbnails") as? Map<*, *>
                    val thumbnailSections = thumbnails?.values as? Collection<Map<*, *>>

                    if (playlistId != null && title != null && videoCount != null && videoCount > 0) {
                        val thumbnail = thumbnailSections?.let { getThumbnail(it) }

                        pojos.add(
                            AlbumPojo(
                                album = Album(
                                    title = title,
                                    isInLibrary = false,
                                    isLocal = false,
                                    albumArt = thumbnail,
                                    youtubePlaylist = YoutubePlaylist(
                                        id = playlistId,
                                        title = title,
                                        thumbnail = thumbnail,
                                        videoCount = videoCount.toInt(),
                                    )
                                ),
                                trackCount = videoCount.toInt(),
                            )
                        )
                    }
                }
            }
        }

        return pojos
    }

    /**
     * Gets list of items (videos) belonging to a playlist, including their positions. Does not get video details;
     * that must be done via listVideoDetails().
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun listPlaylistItems(playlistId: String): List<YoutubePlaylistItem> {
        val urlString = "https://youtube.googleapis.com/youtube/v3/playlistItems?part=snippet" +
            "&part=contentDetails&maxResults=50&playlistId=$playlistId&key=${BuildConfig.youtubeApiKey}"
        var finished = false
        var pageToken: String? = null
        val playlistItems = mutableListOf<YoutubePlaylistItem>()

        while (!finished) {
            val conn = urlRequest(urlString + (pageToken?.let { "&pageToken=$it" } ?: ""))
            val body = withContext(Dispatchers.IO) { conn.getInputStream().use { it.bufferedReader().readText() } }
            val response = gson.fromJson(body, responseType)
            val items = response["items"] as? Collection<Map<*, *>>

            pageToken = response["nextPageToken"] as? String
            finished = pageToken == null
            items?.forEach { item ->
                val playlistItemId = item["id"] as? String
                val videoId = (item["contentDetails"] as? Map<*, *>)?.get("videoId") as? String
                val position = (item["snippet"] as? Map<*, *>)?.get("position") as? Double

                if (playlistItemId != null && videoId != null && position != null) {
                    playlistItems.add(
                        YoutubePlaylistItem(
                            id = playlistItemId,
                            videoId = videoId,
                            position = position.toInt() + 1,
                        )
                    )
                }
            }
        }

        return playlistItems
    }

    private suspend fun listPlaylistVideos(playlist: YoutubePlaylist, withMetadata: Boolean): List<YoutubeVideo> {
        playlistVideoCache[playlist.id]?.let { return it }

        val items = listPlaylistItems(playlist.id)
        val videos = listVideoDetails(videoIds = items.map { it.videoId }, withMetadata = withMetadata)

        return videos
            .zipBy(items) { video, item -> video.id == item.videoId }
            .map { (video, item) ->
                video.copy(playlistItemId = item.id, playlistPosition = item.position)
            }
            .sortedBy { it.playlistPosition }
            .also { playlistVideoCache[playlist.id] = it }
    }

    /**
     * Batch get details for videos with the given IDs.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun listVideoDetails(videoIds: Collection<String>, withMetadata: Boolean): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()

        if (videoIds.isEmpty()) return videos

        // Make sure all ids are unique and then divide into chunks of 50 (max number the API accepts):
        videoIds.toSet().chunked(50).forEach { chunk ->
            val urlString = "https://youtube.googleapis.com/youtube/v3/videos?part=contentDetails&part=snippet" +
                "&id=${chunk.joinToString(",")}&key=${BuildConfig.youtubeApiKey}"
            val conn = urlRequest(urlString)
            val body = conn.getInputStream().use { it.bufferedReader().readText() }
            val response = gson.fromJson(body, responseType)
            val items = response["items"] as? Collection<Map<*, *>>

            items?.forEach { item ->
                val videoId = item["id"] as? String
                val snippet = item["snippet"] as? Map<*, *>
                val contentDetails = item["contentDetails"] as? Map<*, *>
                val title = snippet?.get("title") as? String
                val lengthString = contentDetails?.get("duration") as? String

                if (videoId != null && title != null) {
                    if (withMetadata) {
                        getBestMetadata(videoId)?.let {
                            videos.add(YoutubeVideo(id = videoId, title = title, length = lengthString, metadata = it))
                        }
                    } else {
                        videos.add(YoutubeVideo(id = videoId, title = title, length = lengthString))
                    }
                }
            }
        }

        return videos
    }

    private suspend inline fun reallyDownloadTrack(
        url: String,
        outputStream: OutputStream,
        progressCallback: (Double) -> Unit,
    ) {
        var rangeStart = 0
        var finished = false
        var contentLength: Int? = null

        while (!finished) {
            val conn = urlRequest(
                urlString = url,
                headers = mapOf("Range" to "bytes=$rangeStart-${DOWNLOAD_CHUNK_SIZE + rangeStart}"),
            )
            val contentRange = conn.getHeaderField("Content-Range")?.parseContentRange()
            if (contentLength == null)
                contentLength = contentRange?.size ?: conn.getHeaderField("Content-Length")?.toInt()
            withContext(Dispatchers.IO) { conn.getInputStream().use { outputStream.write(it.readBytes()) } }
            if (contentLength != null) {
                progressCallback(min((DOWNLOAD_CHUNK_SIZE + rangeStart).toDouble() / contentLength, 1.0))
            }
            if (contentRange?.size != null && contentRange.size - contentRange.rangeEnd > 1)
                rangeStart = contentRange.rangeEnd + 1
            else finished = true
        }
    }
}
