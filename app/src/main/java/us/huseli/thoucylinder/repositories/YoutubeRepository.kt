package us.huseli.thoucylinder.repositories

import android.content.ContentValues
import android.content.Context
import android.graphics.ImageDecoder
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.DOWNLOAD_CHUNK_SIZE
import us.huseli.thoucylinder.Constants.HEADER_ANDROID_SDK_VERSION
import us.huseli.thoucylinder.Constants.HEADER_USER_AGENT
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_NAME
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_VERSION
import us.huseli.thoucylinder.ExtractTrackDataException
import us.huseli.thoucylinder.MediaStoreFormatException
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.data.entities.ExtractedTrackData
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubePlaylistItem
import us.huseli.thoucylinder.data.entities.YoutubePlaylistVideo
import us.huseli.thoucylinder.data.entities.YoutubeStreamData
import us.huseli.thoucylinder.data.entities.YoutubeStreamDict
import us.huseli.thoucylinder.data.entities.YoutubeThumbnail
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.data.entities.parseContentRange
import us.huseli.thoucylinder.lengthToDuration
import us.huseli.thoucylinder.yquery
import us.huseli.thoucylinder.zipBy
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder
import kotlin.math.min
import kotlin.time.Duration

object YoutubeRepository {
    private const val CONNECT_TIMEOUT = 4_050
    private const val READ_TIMEOUT = 10_000

    private val gson: Gson = GsonBuilder().create()
    private val responseType = object : TypeToken<Map<String, *>>() {}

    class DownloadStatus(val video: YoutubeVideo? = null, val status: Status = Status.IDLE) {
        enum class Status { IDLE, DOWNLOADING, CONVERTING, MOVING }
    }

    fun downloadPlaylist(
        playlist: YoutubePlaylist,
        context: Context,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus) -> Unit,
    ) {
        playlist.videos.forEachIndexed { itemIdx, item ->
            val uri = downloadTrack(
                video = item.video,
                dirName = "${Environment.DIRECTORY_MUSIC}/${playlist.generateDirName()}",
                context = context,
                trackNumber = item.position + 1,
                progressCallback = { progress ->
                    val playlistProgress = (itemIdx + progress) / playlist.videos.size
                    Log.i("downloadPlaylist", "progress=$progress, playlistProgress=$playlistProgress")
                    progressCallback(playlistProgress)
                },
                statusCallback = { statusCallback(DownloadStatus(item.video, it)) },
                extraContentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.ALBUM, playlist.title)
                    playlist.artist?.let { put(MediaStore.Audio.Media.ARTIST, it) }
                }
            )
            item.video.localUri = uri
        }
    }

    fun downloadTrack(
        video: YoutubeVideo,
        context: Context,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus) -> Unit,
    ) {
        val uri = downloadTrack(
            video = video,
            dirName = Environment.DIRECTORY_MUSIC,
            context = context,
            progressCallback = { progress ->
                Log.i("downloadTrack", "progress=$progress")
                progressCallback(progress)
            },
            statusCallback = { statusCallback(DownloadStatus(video, it)) },
        )
        video.localUri = uri
    }

    fun getPlaylistThumbnail(playlistId: String, url: String, dir: File): ImageBitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val file = File(dir, playlistId)

            if (!file.isFile) {
                val conn = request(url)
                val body = conn.getInputStream().use { it.readBytes() }
                file.outputStream().use { it.write(body) }
            }

            if (file.isFile) {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)).asImageBitmap()
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun getStreamData(videoId: String): YoutubeStreamData {
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
        val conn = request(
            urlString = "https://www.youtube.com/youtubei/v1/player" +
                "?key=AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w&prettyPrint=false",
            headers = mapOf(
                "User-Agent" to HEADER_USER_AGENT,
                "content-type" to "application/json",
                "X-YouTube-Client-Name" to HEADER_X_YOUTUBE_CLIENT_NAME,
                "X-YouTube-Client-Version" to HEADER_X_YOUTUBE_CLIENT_VERSION,
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
        val streamData = YoutubeStreamData()

        formats.plus(adaptiveFormats).forEach { fmt ->
            val mimeType = fmt["mimeType"] as? String
            val bitrate = fmt["bitrate"] as? Double
            val sampleRate = fmt["audioSampleRate"] as? String
            val fmtUrl = fmt["url"] as? String

            if (mimeType != null && bitrate != null && sampleRate != null && fmtUrl != null) {
                streamData.streamDicts.add(
                    YoutubeStreamDict(
                        _mimeType = mimeType,
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
        return streamData
    }

    fun listPlaylistVideos(playlist: YoutubePlaylist): List<YoutubePlaylistVideo> {
        val items = listPlaylistItems(playlist.id)
        val videos = listVideoDetails(items.map { it.videoId })
        return videos
            .zipBy(items) { video, item -> video.id == item.videoId }
            .map { (video, item) ->
                YoutubePlaylistVideo(id = item.id, playlistId = playlist.id, video = video, position = item.position)
            }
            .sortedBy { it.position }
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
                            scrapedPlaylists.add(YoutubePlaylist(artist = artist, title = listTitle, id = playlistId))
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
        // thumbnails and a video count. (Also, search via API costs a bunch of credits.)
        val apiPlaylists = listPlaylistDetails(scrapedPlaylists.map { it.id })
        val playlists = scrapedPlaylists
            .zipBy(apiPlaylists) { a, b -> a.id == b.id }
            .map { (scrapedPlaylist, apiPlaylist) ->
                scrapedPlaylist.copy(videoCount = apiPlaylist.videoCount, thumbnail = apiPlaylist.thumbnail)
            }

        return Pair(playlists, videos)
    }

    /**
     * Extract metadata from audio file with MediaExtractor and ffmpeg.
     * @throws ExtractTrackDataException
     */
    private fun extractTrackData(file: File): ExtractedTrackData {
        val extractor = MediaExtractor()
        val ff = FFprobeKit.getMediaInformation(file.path)?.mediaInformation
        extractor.setDataSource(file.path)

        for (trackIdx in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackIdx)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val codec = MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(format)

            if (mimeType?.startsWith("audio/") == true) {
                val ffStream = ff?.streams?.getOrNull(trackIdx)
                val extension =
                    when {
                        ffStream?.codec != null && ff.format?.contains(",") == true -> ffStream.codec
                        ff?.format != null -> ff.format
                        else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                            ?: mimeType.split("/").last().lowercase()
                    }

                extractor.release()
                return ExtractedTrackData(
                    mimeType = mimeType,
                    durationUs = durationUs,
                    codec = codec,
                    ffFormat = ff?.format,
                    ffCodec = ffStream?.codec,
                    extension = extension,
                )
            }
        }
        extractor.release()
        throw ExtractTrackDataException(file, extractor, ff)
    }

    private fun reallyDownloadTrack(url: String, outputStream: OutputStream, progressCallback: (Double) -> Unit) {
        var rangeStart = 0
        var finished = false
        var contentLength: Int? = null

        while (!finished) {
            val conn = request(
                urlString = url,
                headers = mapOf("Range" to "bytes=$rangeStart-${DOWNLOAD_CHUNK_SIZE + rangeStart}"),
            )
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
    }

    /**
     * @throws MediaStoreFormatException
     */
    private fun moveFileToMediaStore(
        file: File,
        dirname: String,
        basename: String,
        context: Context,
        extraContentValues: ContentValues? = null,
    ): Uri? {
        val resolver = context.contentResolver
        val audioCollection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val trackData = extractTrackData(file)
        val filename = "$basename.${trackData.extension}"

        // If file already exists, just delete it first.
        File(File(Environment.getExternalStorageDirectory(), dirname), filename).delete()

        val trackDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, dirname)
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, trackData.mimeType)
            extraContentValues?.let { putAll(it) }
            put(MediaStore.Audio.Media.DURATION, (trackData.durationUs / 1000).toInt())
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val trackUri = try {
            resolver.insert(audioCollection, trackDetails)
        } catch (e: IllegalArgumentException) {
            Log.e("downloadTrack", e.toString(), e)
            throw MediaStoreFormatException(filename, trackDetails)
        }

        if (trackUri != null) {
            resolver.openOutputStream(trackUri, "wt")?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    outputStream.write(inputStream.readBytes())
                }
            }
            file.delete()
            trackDetails.clear()
            trackDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(trackUri, trackDetails, null, null)
            return trackUri
        }
        return null
    }

    /**
     * Used both for downloading individual tracks/videos and albums/playlists.
     * @throws TrackDownloadException
     */
    private fun downloadTrack(
        video: YoutubeVideo,
        dirName: String,
        context: Context,
        trackNumber: Int? = null,
        extraContentValues: ContentValues? = null,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus.Status) -> Unit,
    ): Uri {
        val streamUrl = video.getBestStreamDict()?.url
        val basename = video.generateBasename(trackNumber)
        val tempFile = File(context.cacheDir, video.id)

        if (streamUrl == null) throw TrackDownloadException(video, TrackDownloadException.ErrorType.NO_STREAM_URL)
        statusCallback(DownloadStatus.Status.DOWNLOADING)

        try {
            reallyDownloadTrack(
                url = streamUrl,
                outputStream = tempFile.outputStream(),
                progressCallback = progressCallback,
            )
        } catch (e: Exception) {
            throw TrackDownloadException(video, TrackDownloadException.ErrorType.DOWNLOAD, cause = e)
        }

        return try {
            statusCallback(DownloadStatus.Status.MOVING)
            moveFileToMediaStore(
                file = tempFile,
                dirname = dirName,
                basename = basename,
                context = context,
                extraContentValues = extraContentValues,
            )
        } catch (e: MediaStoreFormatException) {
            statusCallback(DownloadStatus.Status.CONVERTING)
            val session = FFmpegKit.execute("-i ${tempFile.path} -vn ${tempFile.path}.opus")
            tempFile.delete()
            if (!session.returnCode.isValueSuccess)
                throw TrackDownloadException(video, TrackDownloadException.ErrorType.FFMPEG_CONVERT)
            statusCallback(DownloadStatus.Status.MOVING)
            moveFileToMediaStore(
                file = File(tempFile.path + ".opus"),
                dirname = dirName,
                basename = basename,
                context = context,
                extraContentValues = extraContentValues,
            )
        } catch (e: ExtractTrackDataException) {
            throw TrackDownloadException(video, TrackDownloadException.ErrorType.EXTRACT_TRACK_DATA, cause = e)
        } ?: throw TrackDownloadException(video, TrackDownloadException.ErrorType.MEDIA_STORE)
    }

    /**
     * Extracts thumbnail URL from API response, works for both playlists and videos.
     */
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

    /**
     * Fetches playlist details (including video counts but _not_ video details) from API.
     */
    @Suppress("UNCHECKED_CAST")
    private fun listPlaylistDetails(playlistIds: Collection<String>): List<YoutubePlaylist> {
        val playlists = mutableListOf<YoutubePlaylist>()

        if (playlistIds.isEmpty()) return playlists

        playlistIds.toSet().chunked(50).forEach { chunk ->
            val urlString = "https://youtube.googleapis.com/youtube/v3/playlists?part=snippet" +
                "&part=contentDetails&maxResults=50" +
                "&id=${chunk.joinToString(",")}&key=${BuildConfig.youtubeApiKey}"
            var finished = false
            var pageToken: String? = null

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
                    val videoCount = item.yquery<Double>("contentDetails.itemCount")

                    if (playlistId != null && title != null && videoCount != null && videoCount > 0) {
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
    private fun listPlaylistItems(playlistId: String): List<YoutubePlaylistItem> {
        val urlString = "https://youtube.googleapis.com/youtube/v3/playlistItems?part=snippet" +
            "&part=contentDetails&maxResults=50&playlistId=$playlistId&key=${BuildConfig.youtubeApiKey}"
        var finished = false
        var pageToken: String? = null
        val playlistItems = mutableListOf<YoutubePlaylistItem>()

        while (!finished) {
            val conn = request(urlString + (pageToken?.let { "&pageToken=$it" } ?: ""))
            val body = conn.getInputStream().use { it.bufferedReader().readText() }
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
    private fun listVideoDetails(videoIds: Collection<String>): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()

        if (videoIds.isEmpty()) return videos

        // Make sure all ids are unique and then divide into chunks of 50 (max number the API accepts):
        videoIds.toSet().chunked(50).forEach { chunk ->
            val urlString = "https://youtube.googleapis.com/youtube/v3/videos?part=contentDetails&part=snippet" +
                "&id=${chunk.joinToString(",")}&key=${BuildConfig.youtubeApiKey}"
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

    private fun request(
        urlString: String,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
    ): URLConnection {
        Log.i("YoutubeRepository", "request: $urlString")
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
}
