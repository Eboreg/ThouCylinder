package us.huseli.thoucylinder.repositories

import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubeStreamDict
import us.huseli.thoucylinder.data.entities.YoutubeThumbnail
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.lengthToDuration
import us.huseli.thoucylinder.sanitizeFilename
import us.huseli.thoucylinder.yquery
import us.huseli.thoucylinder.zipEquals
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import kotlin.time.Duration

object YoutubeRepository {
    private const val CONNECT_TIMEOUT = 4_050
    private const val READ_TIMEOUT = 10_000

    private val gson: Gson = GsonBuilder().create()
    private val responseType = object : TypeToken<Map<String, *>>() {}

    private fun <T> yquery(data: Map<*, *>, keys: String, failSilently: Boolean = true): T? {
        val splitKeys = keys.split(".", limit = 2)
        val key = splitKeys[0]
        val newKeys = splitKeys.getOrNull(1) ?: ""
        val value = data[key]

        if (newKeys.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            (value as? T)?.let { return it } ?: run { throw Exception("$value is not of correct type") }
        }
        if (value is Map<*, *>) {
            return yquery<T>(data = value, keys = newKeys, failSilently = failSilently)
        }
        if (!failSilently) {
            if (!data.containsKey(key)) throw Exception("Key $key does not exist in $data.")
            throw Exception("$key exists, but is not a dict: $data.")
        }
        return null
    }

    fun search(query: String): Pair<List<YoutubePlaylist>, List<YoutubeVideo>> {
        val scrapedPlaylists = mutableListOf<YoutubePlaylist>()
        val videos = mutableListOf<YoutubeVideo>()
        val conn = request(
            urlString = "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}",
            headers = mapOf(
                "User-Agent" to "kuken/1.2.3",
                "Accept" to "*/*",
            )
        )
        val body = conn.getInputStream().use { it.bufferedReader().readText() }
        val ytDataJson = Regex("var ytInitialData *= *(\\{.*?\\});", RegexOption.MULTILINE)
            .find(body)
            ?.groupValues
            ?.lastOrNull()
        val ytData = ytDataJson?.let { gson.fromJson(it, responseType) }

        if (ytData != null) {
            ytData.yquery<Collection<Map<*, *>>>(
                "contents.twoColumnSearchResultsRenderer.secondaryContents.secondarySearchContainerRenderer.contents"
            )?.forEach { content ->
                content.yquery<Map<*, *>>("universalWatchCardRenderer.header.watchCardRichHeaderRenderer")?.let { header ->
                    val listTitle = header.yquery<String>("title.simpleText")
                    val listSubtitle = header.yquery<String>("subtitle.simpleText")
                    val playlistId =
                        header.yquery<String>("titleNavigationEndpoint.commandMetadata.webCommandMetadata.url")?.split("=")?.last()

                    if (listTitle != null && playlistId != null) {
                        scrapedPlaylists.add(YoutubePlaylist(subtitle = listSubtitle, title = listTitle, id = playlistId))
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

                        if (listTitle != null && playlistId != null && !scrapedPlaylists.map { it.id }.contains(playlistId)) {
                            scrapedPlaylists.add(YoutubePlaylist(title = listTitle, id = playlistId))
                        }
                    } else if (section.containsKey("videoRenderer")) {
                        val videoId = section.yquery<String>("videoRenderer.videoId")
                        val title =
                            section.yquery<Collection<Map<*, *>>>("videoRenderer.title.runs")
                                ?.firstOrNull()?.get("text") as? String
                        val length =
                            section.yquery<String>("videoRenderer.lengthText.simpleText")?.lengthToDuration()

                        if (videoId != null && title != null)
                            videos.add(YoutubeVideo(title = title, id = videoId, length = length))
                    }
                }
            }
        }

        // We need to get playlists using both methods (this and via the regular API) because this one is the only one
        // that gives us "subtitle" (for secondaryContents), which may contain artist name, and the API gives us
        // thumbnails and all the songs.
        val apiPlaylists = listPlaylistDetails(scrapedPlaylists.map { it.id })
        val combined = scrapedPlaylists.zipEquals(apiPlaylists) { a, b -> a.id == b.id }
        val playlists = combined.map { (scrapedPlaylist, apiPlaylist) ->
            scrapedPlaylist.copy(videos = apiPlaylist.videos, thumbnail = apiPlaylist.thumbnail)
        }

        return Pair(playlists, videos)
    }

    @Suppress("UNCHECKED_CAST")
    fun getStreamDict(videoId: String): YoutubeStreamDict? {
        var best: YoutubeStreamDict? = null

        val gson: Gson = GsonBuilder().create()
        val data: Map<String, *> = mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to "ANDROID",
                    "clientVersion" to "17.31.35",
                    "androidSdkVersion" to 30,
                    "userAgent" to "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip",
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
        val conn = request(
            urlString = "https://www.youtube.com/youtubei/v1/player?key=AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w&prettyPrint=false",
            headers = mapOf(
                "User-Agent" to "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip",
                "content-type" to "application/json",
                "X-YouTube-Client-Name" to "3",
                "X-YouTube-Client-Version" to "17.31.35",
                "Origin" to "https://www.youtube.com",
            ),
            body = requestBody.toByteArray(Charsets.UTF_8),
        )
        val body = conn.getInputStream().use { it.bufferedReader().readText() }
        val response = gson.fromJson(body, responseType)
        val formats =
            (response["streamingData"] as? Map<*, *>)?.get("formats") as? Collection<Map<*, *>> ?: emptyList()
        val adaptiveFormats =
            (response["streamingData"] as? Map<*, *>)?.get("adaptiveFormats") as? Collection<Map<*, *>> ?: emptyList()

        formats.plus(adaptiveFormats).forEach { fmt ->
            val mimeType = fmt["mimeType"] as? String
            val bitrate = fmt["bitrate"] as? Double
            val sampleRate = fmt["audioSampleRate"] as? String
            val fmtUrl = fmt["url"] as? String

            if (mimeType != null && bitrate != null && sampleRate != null && fmtUrl != null && mimeType.startsWith("audio")) {
                val streamDict = YoutubeStreamDict(mimeType = mimeType, bitrate = bitrate.toInt(), sampleRate = sampleRate.toInt(), url = fmtUrl)
                if (best == null || streamDict.quality > best!!.quality) best = streamDict
            }
        }
        return best
    }

    fun downloadTrack(video: YoutubeVideo, dir: File, filename: String? = null) {
        video.streamDict?.let { streamDict ->
            val file = File(dir, filename?.let { sanitizeFilename(it) } ?: video.generateFilename())
            var rangeStart = 0
            var finished = false

            file.outputStream().use { fileStream ->
                while (!finished) {
                    val conn = request(
                        urlString = streamDict.url,
                        headers = mapOf(
                            "Range" to "bytes=$rangeStart-",
                        ),
                    )
                    val contentRange = conn.getHeaderField("Content-Range")
                    val regex = Regex("bytes \\d+-(\\d+)/(\\d+)")

                    conn.getInputStream().use { fileStream.write(it.readBytes()) }
                    regex.find(contentRange)?.groupValues?.let {
                        val end = it[1].toInt()
                        val total = it[2].toInt()
                        if (total - end > 1) rangeStart = end + 1
                        else finished = true
                    } ?: kotlin.run { finished = true }
                }
            }
        }
    }

    private fun request(urlString: String, headers: Map<String, String> = emptyMap(), body: ByteArray? = null): URLConnection {
        return URL(urlString).openConnection().apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) {
                doOutput = true
                getOutputStream().write(body, 0, body.size)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun listPlaylistDetails(playlistIds: Collection<String>): List<YoutubePlaylist> {
        /**
         * Minimum (2 + playlistIds.size) API requests at 1 credit each, more if playlistIds.size > 50 or the total
         * number of videos exceeds 50.
         */
        val urlString = "https://youtube.googleapis.com/youtube/v3/playlists?part=snippet&maxResults=50&id=${playlistIds.joinToString(",")}&key=${BuildConfig.youtubeApiKey}"
        var finished = false
        var pageToken: String? = null
        val playlists = mutableListOf<YoutubePlaylist>()

        while (!finished) {
            val conn = request(urlString + (pageToken?.let { "&pageToken=$it" } ?: ""))
            val body = conn.getInputStream().use { it.bufferedReader().readText() }
            val response = gson.fromJson(body, responseType)
            val items = response["items"] as? Collection<Map<*, *>>

            pageToken = response["nextPageToken"] as? String
            finished = pageToken == null
            items?.forEach { item ->
                val playlistId = item["id"] as? String
                val snippet = item["snippet"] as? Map<*, *>
                val thumbnail = getThumbnail(snippet?.get("thumbnails"))
                val title = snippet?.get("title") as? String

                if (playlistId != null && title != null) {
                    playlists.add(
                        YoutubePlaylist(
                            title = title,
                            id = playlistId,
                            thumbnail = thumbnail,
                        )
                    )
                }
            }
        }

        val playlistVideos = listVideoDetails(playlists.flatMap { it.videoIds })
        playlists.forEach { playlist ->
            playlist.videos = playlistVideos.filter { playlist.videoIds.contains(it.id) }
        }
        return playlists
    }

    @Suppress("UNCHECKED_CAST")
    private fun listVideoDetails(videoIds: Collection<String>): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()

        // Make sure all ids are unique and then divide into chunks of 50 (max number the API accepts):
        videoIds.toSet().chunked(50).forEach { chunk ->
            val urlString = "https://youtube.googleapis.com/youtube/v3/videos?part=contentDetails&part=snippet&id=${chunk.joinToString(",")}&key=${BuildConfig.youtubeApiKey}"
            val conn = request(urlString)
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
                            length = lengthString?.let { Duration.parse(it) },
                            thumbnail = getThumbnail(snippet["thumbnails"]),
                        )
                    )
                }
            }
        }

        return videos
    }

    @Suppress("UNCHECKED_CAST")
    private fun getThumbnail(section: Any?): YoutubeThumbnail? {
        var best: YoutubeThumbnail? = null
        ((section as? Map<*, *>)?.values?.toList() as? Collection<Map<*, *>>)?.forEach {
            val tnUrl = it["url"] as? String
            val tnWidth = it["width"] as? Double
            val tnHeight = it["height"] as? Double
            if (tnUrl != null && tnWidth != null && tnHeight != null) {
                val tn = YoutubeThumbnail(url = tnUrl, width = tnWidth.toInt(), height = tnHeight.toInt())
                if (best == null || best!!.size < tn.size) best = tn
            }
        }
        return best
    }

    @Suppress("UNCHECKED_CAST")
    private fun listPlaylistVideoIds(playlistId: String): List<String> {
        val urlString = "https://youtube.googleapis.com/youtube/v3/playlistItems?part=contentDetails&maxResults=50&playlistId=$playlistId&key=${BuildConfig.youtubeApiKey}"
        var finished = false
        var pageToken: String? = null
        val videoIds = mutableListOf<String>()

        while (!finished) {
            val conn = request(urlString + (pageToken?.let { "&pageToken=$it" } ?: ""))
            val body = conn.getInputStream().use { it.bufferedReader().readText() }
            val response = gson.fromJson(body, responseType)
            val items = response["items"] as? Collection<Map<*, *>>

            pageToken = response["nextPageToken"] as? String
            finished = pageToken == null
            items?.forEach { item ->
                ((item["contentDetails"] as? Map<*, *>)?.get("videoId") as? String)?.let { videoIds.add(it) }
            }
        }

        return videoIds
    }

    fun getPlaylistThumbnail(playlist: YoutubePlaylist, dir: File): ImageBitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val file = File(dir, playlist.id)

            if (!file.isFile) {
                playlist.thumbnail?.url?.let { url ->
                    val conn = request(url)
                    val body = conn.getInputStream().use { it.readBytes() }
                    file.outputStream().use { it.write(body) }
                }
            }

            if (file.isFile) {
                val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file))
                bitmap.config
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)).asImageBitmap()
            }
        }
        return null
    }
}
