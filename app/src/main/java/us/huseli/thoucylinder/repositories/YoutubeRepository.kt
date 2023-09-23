package us.huseli.thoucylinder.repositories

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.DOWNLOAD_CHUNK_SIZE
import us.huseli.thoucylinder.Constants.HEADER_ANDROID_SDK_VERSION
import us.huseli.thoucylinder.Constants.HEADER_USER_AGENT
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_NAME
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_VERSION
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_EXCLUDE
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_FILTER
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.dataclasses.TempAlbum
import us.huseli.thoucylinder.dataclasses.TempTrack
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.YoutubeMetadataList
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.YoutubePlaylistItem
import us.huseli.thoucylinder.dataclasses.YoutubePlaylistVideo
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.extrackTrackMetadata
import us.huseli.thoucylinder.urlRequest
import us.huseli.thoucylinder.yquery
import us.huseli.thoucylinder.zipBy
import java.io.File
import java.io.OutputStream
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class YoutubeRepository @Inject constructor(@ApplicationContext private val context: Context) {
    // Written over for every new search:
    private val _playlistSearchResults = MutableStateFlow<List<YoutubePlaylist>>(emptyList())
    private val _videoSearchResults = MutableStateFlow<List<YoutubeVideo>>(emptyList())
    private val _playlistVideos = mutableMapOf<String, List<YoutubePlaylistVideo>>()

    private val gson: Gson = GsonBuilder().create()
    private val responseType = object : TypeToken<Map<String, *>>() {}
    private val metadataMap = mutableMapOf<String, YoutubeMetadataList>()
    private val thumbnailDir = File(context.filesDir, "thumbnails").apply { mkdirs() }

    val playlistSearchResults = _playlistSearchResults.asStateFlow()
    val videoSearchResults = _videoSearchResults.asStateFlow()

    suspend fun downloadPlaylist(
        playlist: YoutubePlaylist,
        videos: List<YoutubePlaylistVideo>,
        progressCallback: (DownloadProgress) -> Unit,
    ): TempAlbum {
        val tracks = mutableListOf<TempTrack>()

        videos.forEachIndexed { itemIdx, item ->
            val (file, metadata) = downloadVideo(video = item.video) {
                progressCallback(it.copy(progress = (itemIdx + it.progress) / videos.size))
            }
            tracks.add(item.toTempTrack(localFile = file, metadata = metadata))
        }

        return playlist.toTempAlbum(tracks = tracks)
    }

    suspend fun downloadTrack(video: YoutubeVideo, statusCallback: (DownloadProgress) -> Unit): TempTrack {
        val (file, metadata) = downloadVideo(video = video, statusCallback = statusCallback)
        return video.toTempTrack(localFile = file, metadata = metadata)
    }

    suspend fun getBestMetadata(videoId: String): YoutubeMetadata? =
        getMetadataList(videoId).getBest(VIDEO_MIMETYPE_FILTER, VIDEO_MIMETYPE_EXCLUDE)

    suspend fun getPlaylist(playlistId: String): YoutubePlaylist? =
        _playlistSearchResults.value.find { it.id == playlistId } ?: getPlaylistDetails(playlistId)

    suspend fun listPlaylistVideos(playlist: YoutubePlaylist): List<YoutubePlaylistVideo> {
        _playlistVideos[playlist.id]?.let { return it }

        val items = listPlaylistItems(playlist.id)
        val videos = listVideoDetails(items.map { it.videoId })

        return videos
            .zipBy(items) { video, item -> video.id == item.videoId }
            .map { (video, item) ->
                YoutubePlaylistVideo(id = item.id, playlistId = playlist.id, video = video, position = item.position)
            }
            .sortedBy { it.position }
            .also { _playlistVideos[playlist.id] = it }
    }

    suspend fun search(query: String) {
        val scrapedPlaylists = mutableListOf<YoutubePlaylist>()
        val videos = mutableListOf<YoutubeVideo>()
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

                            scrapedPlaylists.add(YoutubePlaylist(artist = artist, title = title, id = playlistId))
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

                        if (
                            listTitle != null
                            && playlistId != null
                            && !scrapedPlaylists.map { it.id }.contains(playlistId)
                        ) scrapedPlaylists.add(YoutubePlaylist(title = listTitle, id = playlistId))
                    } else if (section.containsKey("videoRenderer")) {
                        val videoId = section.yquery<String>("videoRenderer.videoId")
                        val title =
                            section.yquery<Collection<Map<*, *>>>("videoRenderer.title.runs")
                                ?.firstOrNull()?.get("text") as? String
                        val length = section.yquery<String>("videoRenderer.lengthText.simpleText")

                        if (videoId != null && title != null)
                            videos.add(YoutubeVideo(title = title, id = videoId, length = length))
                    }
                }
            }
        }

        // We need to get playlists using both methods (this and via the regular API) because this one is the only one
        // that gives us "subtitle" (for secondaryContents), which may contain artist name, and the API gives us
        // thumbnails and a video count. (Also, search via API costs a bunch of credits.)
        val apiPlaylists = listPlaylistDetails(scrapedPlaylists.map { it.id })
        val playlists = scrapedPlaylists
            .zipBy(apiPlaylists) { a, b -> a.id == b.id }
            .map { (scrapedPlaylist, apiPlaylist) ->
                scrapedPlaylist.copy(videoCount = apiPlaylist.videoCount, thumbnail = apiPlaylist.thumbnail)
            }

        _playlistSearchResults.value = playlists
        _videoSearchResults.value = videos
    }

    /**
     * Used both for downloading individual tracks/videos and albums/playlists.
     * @throws TrackDownloadException
     */
    private suspend fun downloadVideo(
        video: YoutubeVideo,
        statusCallback: (DownloadProgress) -> Unit,
    ): Pair<File, TrackMetadata> {
        val streamUrl = getBestMetadata(video.id)?.url
        val tempFile = File(context.cacheDir, video.id)
        val downloadProgress = DownloadProgress(
            item = video.title,
            status = DownloadProgress.Status.DOWNLOADING,
            progress = 0.0,
        )

        if (streamUrl == null) throw TrackDownloadException(TrackDownloadException.ErrorType.NO_STREAM_URL)
        statusCallback(downloadProgress)

        try {
            reallyDownloadTrack(
                url = streamUrl,
                outputStream = tempFile.outputStream(),
                progressCallback = {
                    statusCallback(downloadProgress.copy(progress = it))
                },
            )
        } catch (e: Exception) {
            throw TrackDownloadException(TrackDownloadException.ErrorType.DOWNLOAD, cause = e)
        }

        return Pair(tempFile, tempFile.extrackTrackMetadata())
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getMetadataList(videoId: String): YoutubeMetadataList {
        metadataMap[videoId]?.let { return it }

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
            val fmtUrl = fmt["url"] as? String

            if (mimeType != null && bitrate != null && sampleRate != null && fmtUrl != null) {
                metadataList.metadata.add(
                    YoutubeMetadata(
                        mimeType = mimeType,
                        bitrate = bitrate.toInt(),
                        sampleRate = sampleRate.toInt(),
                        url = fmtUrl,
                        size = (fmt["contentLength"] as? String)?.toInt(),
                        channels = (fmt["audioChannels"] as? Double)?.toInt(),
                        loudnessDb = fmt["loudnessDb"] as? Double,
                    )
                )
            }
        }
        metadataMap[videoId] = metadataList
        return metadataList
    }

    /**
     * Extracts thumbnail URL from API response, works for both playlists and videos.
     * Does not fetch the actual image.
     */
    @Suppress("UNCHECKED_CAST")
    private fun getThumbnail(section: Any?, filename: String): Image? {
        var best: Image? = null

        ((section as? Map<*, *>)?.values?.toList() as? Collection<Map<*, *>>)?.forEach {
            val tnUrl = it["url"] as? String
            val tnWidth = it["width"] as? Double
            val tnHeight = it["height"] as? Double
            if (tnUrl != null && tnWidth != null && tnHeight != null) {
                val tn = Image(
                    url = tnUrl,
                    width = tnWidth.toInt(),
                    height = tnHeight.toInt(),
                    localFile = File(thumbnailDir, filename)
                )
                if (best == null || best!!.size < tn.size) best = tn
            }
        }
        return best
    }

    private suspend fun getPlaylistDetails(playlistId: String): YoutubePlaylist? =
        listPlaylistDetails(listOf(playlistId)).find { it.id == playlistId }

    /**
     * Fetches playlist details (including video counts but _not_ video details) from API.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun listPlaylistDetails(playlistIds: Collection<String>): List<YoutubePlaylist> {
        val playlists = mutableListOf<YoutubePlaylist>()

        if (playlistIds.isEmpty()) return playlists

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

                    if (playlistId != null && title != null && videoCount != null && videoCount > 0) {
                        val thumbnail = getThumbnail(snippet["thumbnails"], playlistId)

                        playlists.add(
                            YoutubePlaylist(
                                title = title,
                                id = playlistId,
                                thumbnail = thumbnail,
                                videoCount = videoCount.toInt(),
                            )
                        )
                    }
                }
            }
        }

        return playlists
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
                            playlistId = playlistId,
                            position = position.toInt(),
                        )
                    )
                }
            }
        }

        return playlistItems
    }

    /**
     * Batch get details for videos with the given IDs.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun listVideoDetails(videoIds: Collection<String>): List<YoutubeVideo> {
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
                    videos.add(
                        YoutubeVideo(
                            id = videoId,
                            title = title,
                            length = lengthString,
                        )
                    )
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
