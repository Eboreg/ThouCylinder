package us.huseli.thoucylinder.repositories

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.DOWNLOAD_CHUNK_SIZE
import us.huseli.thoucylinder.Constants.HEADER_ANDROID_SDK_VERSION
import us.huseli.thoucylinder.Constants.HEADER_USER_AGENT
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_NAME
import us.huseli.thoucylinder.Constants.HEADER_X_YOUTUBE_CLIENT_VERSION
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_EXCLUDE
import us.huseli.thoucylinder.Constants.VIDEO_MIMETYPE_FILTER
import us.huseli.thoucylinder.DownloadStatus
import us.huseli.thoucylinder.ExtractTrackDataException
import us.huseli.thoucylinder.MediaStoreFormatException
import us.huseli.thoucylinder.TrackDownloadException
import us.huseli.thoucylinder.data.entities.Album
import us.huseli.thoucylinder.data.entities.ExtractedTrackData
import us.huseli.thoucylinder.data.entities.Image
import us.huseli.thoucylinder.data.entities.Track
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubePlaylistItem
import us.huseli.thoucylinder.data.entities.YoutubePlaylistVideo
import us.huseli.thoucylinder.data.entities.YoutubeStreamData
import us.huseli.thoucylinder.data.entities.YoutubeStreamDict
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.data.entities.parseContentRange
import us.huseli.thoucylinder.urlRequest
import us.huseli.thoucylinder.yquery
import us.huseli.thoucylinder.zipBy
import java.io.File
import java.io.OutputStream
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class YoutubeRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson: Gson = GsonBuilder().create()
    private val responseType = object : TypeToken<Map<String, *>>() {}
    private val streamDataMap = mutableMapOf<String, YoutubeStreamData>()
    private val thumbnailDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
    private val localMusicDir: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).apply { mkdirs() }
    private val audioCollection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun downloadPlaylist(
        playlist: YoutubePlaylist,
        videos: List<YoutubePlaylistVideo>,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus) -> Unit,
    ): Album {
        val albumId = UUID.randomUUID()
        val tracks = mutableListOf<Track>()

        videos.forEachIndexed { itemIdx, item ->
            val localPath = downloadTrack(
                video = item.video,
                subdirName = playlist.generateSubdirName(),
                trackNumber = item.position + 1,
                progressCallback = { progress ->
                    val playlistProgress = (itemIdx + progress) / videos.size
                    Log.i("downloadPlaylist", "progress=$progress, playlistProgress=$playlistProgress")
                    progressCallback(playlistProgress)
                },
                statusCallback = { statusCallback(DownloadStatus(item.video.title, it)) },
                extraContentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.ALBUM, playlist.title)
                    playlist.artist?.let { put(MediaStore.Audio.Media.ARTIST, it) }
                }
            )
            tracks.add(
                item.video.toTrack(
                    artist = playlist.artist,
                    albumId = albumId,
                    localPath = localPath,
                    albumPosition = item.position,
                )
            )
        }
        return playlist.toAlbum(albumId = albumId, tracks = tracks)
    }

    suspend fun downloadTrack(
        video: YoutubeVideo,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus) -> Unit,
    ): Track {
        val path = downloadTrack(
            video = video,
            subdirName = "",
            progressCallback = { progress ->
                Log.i("downloadTrack", "progress=$progress")
                progressCallback(progress)
            },
            statusCallback = { statusCallback(DownloadStatus(video.title, it)) },
        )
        return video.toTrack(localPath = path)
    }

    suspend fun getBestStreamDict(videoId: String): YoutubeStreamDict? =
        getStreamData(videoId).getBestStreamDict(VIDEO_MIMETYPE_FILTER, VIDEO_MIMETYPE_EXCLUDE)

    suspend fun listPlaylistVideos(playlist: YoutubePlaylist): List<YoutubePlaylistVideo> {
        val items = listPlaylistItems(playlist.id)
        val videos = listVideoDetails(items.map { it.videoId })
        return videos
            .zipBy(items) { video, item -> video.id == item.videoId }
            .map { (video, item) ->
                YoutubePlaylistVideo(id = item.id, playlistId = playlist.id, video = video, position = item.position)
            }
            .sortedBy { it.position }
    }

    suspend fun search(query: String): Pair<List<YoutubePlaylist>, List<YoutubeVideo>> {
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

        return Pair(playlists, videos)
    }

    /**
     * Used both for downloading individual tracks/videos and albums/playlists.
     * @throws TrackDownloadException
     */
    private suspend inline fun downloadTrack(
        video: YoutubeVideo,
        subdirName: String,
        trackNumber: Int? = null,
        extraContentValues: ContentValues? = null,
        progressCallback: (Double) -> Unit,
        statusCallback: (DownloadStatus.Status) -> Unit,
    ): String {
        val streamUrl = getBestStreamDict(video.id)?.url
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
                subdirName = subdirName,
                basename = basename,
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
                subdirName = subdirName,
                basename = basename,
                extraContentValues = extraContentValues,
            )
        } catch (e: ExtractTrackDataException) {
            throw TrackDownloadException(video, TrackDownloadException.ErrorType.EXTRACT_TRACK_DATA, cause = e)
        } ?: throw TrackDownloadException(video, TrackDownloadException.ErrorType.MEDIA_STORE)
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

    @Suppress("UNCHECKED_CAST")
    private suspend fun getStreamData(videoId: String): YoutubeStreamData {
        streamDataMap[videoId]?.let { return it }

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
        streamDataMap[videoId] = streamData
        return streamData
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

    private fun deleteExistingMediaFile(filename: String, subdirName: String) {
        val subdir = File(localMusicDir, subdirName)
        val dirName = "${Environment.DIRECTORY_MUSIC}/" + if (subdirName.isNotEmpty()) "$subdirName/" else ""
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(filename, dirName)

        File(subdir, filename).delete()
        if (subdirName.isNotEmpty() && subdir.list()?.isEmpty() == true)
            subdir.delete()

        context.contentResolver.query(audioCollection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                )
                context.contentResolver.delete(uri, null, null)
            }
        }
    }

    /**
     * @throws MediaStoreFormatException
     */
    private fun moveFileToMediaStore(
        file: File,
        subdirName: String,
        basename: String,
        extraContentValues: ContentValues? = null,
    ): String? {
        val trackData = extractTrackData(file)
        val filename = "$basename.${trackData.extension}"

        // If file already exists, just delete it first.
        deleteExistingMediaFile(filename, subdirName)

        val trackDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$subdirName")
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, trackData.mimeType)
            extraContentValues?.let { putAll(it) }
            put(MediaStore.Audio.Media.DURATION, (trackData.durationUs / 1000).toInt())
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val trackUri = try {
            context.contentResolver.insert(audioCollection, trackDetails)
        } catch (e: IllegalArgumentException) {
            Log.e("downloadTrack", e.toString(), e)
            throw MediaStoreFormatException(filename, trackDetails)
        }

        if (trackUri != null) {
            context.contentResolver.openOutputStream(trackUri, "wt")?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    outputStream.write(inputStream.readBytes())
                }
            }
            file.delete()
            trackDetails.clear()
            trackDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(trackUri, trackDetails, null, null)
            return "$subdirName/$filename"
        }
        return null
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
