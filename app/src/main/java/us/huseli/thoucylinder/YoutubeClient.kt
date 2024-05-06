package us.huseli.thoucylinder

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import us.huseli.retaintheme.extensions.mergeWith
import us.huseli.retaintheme.extensions.toDuration
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_PX
import us.huseli.thoucylinder.dataclasses.combos.YoutubePlaylistCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeImage
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.enums.Region
import us.huseli.thoucylinder.interfaces.ILogger
import java.util.regex.Pattern
import kotlin.math.absoluteValue

class PlaylistSearch(val query: String, private val client: AbstractYoutubeClient) {
    private var nextToken: String? = null
    private var primaryDone: Boolean = false
    private var secondaryDone: Boolean = false
    private val usedPlaylistIds: MutableList<String> = mutableListOf()
    private val _hasMore = MutableStateFlow(true)

    val hasMore = _hasMore.asStateFlow()

    fun flowResults(limit: Int = 50): Flow<YoutubePlaylist> = flow {
        var emitted = 0

        if (!primaryDone) {
            flowPrimaryResults().collect { playlist ->
                emit(playlist)
                emitted++
            }
            primaryDone = true
        }

        if (!secondaryDone && emitted < limit) {
            flowSecondaryResults().collect { playlist ->
                emit(playlist)
                emitted++
            }
            secondaryDone = true
        }

        while (nextToken != null && emitted < limit) {
            nextToken?.also {
                flowContinuationResults(it).collect { playlist ->
                    emit(playlist)
                    emitted++
                }
            }
        }
    }

    private fun flowPrimaryResults(): Flow<YoutubePlaylist> = flow {
        getAndEmit(client.getPlaylistSearchResult(query = query, playlistSpecificSearch = true))
    }

    private fun flowSecondaryResults(): Flow<YoutubePlaylist> = flow {
        val result = client.getPlaylistSearchResult(query = query, playlistSpecificSearch = false)

        getAndEmit(result)
        _hasMore.value = result.nextToken != null
    }

    private fun flowContinuationResults(continuationToken: String): Flow<YoutubePlaylist> = flow {
        val result = client.getNextPlaylistSearchResult(continuationToken)

        getAndEmit(result)
        _hasMore.value = result.nextToken != null
    }

    private suspend fun FlowCollector<YoutubePlaylist>.getAndEmit(result: AbstractYoutubeClient.PlaylistSearchResult) {
        for (playlist in result.playlists) {
            if (!usedPlaylistIds.contains(playlist.id)) {
                usedPlaylistIds.add(playlist.id)
                emit(playlist)
            }
        }
        nextToken = result.nextToken
    }
}

abstract class AbstractYoutubeClient(val region: Region = Region.SE) : ILogger {
    abstract val clientName: String
    abstract val clientVersion: String
    abstract val key: String

    data class PlaylistSearchResult(
        val playlists: ImmutableList<YoutubePlaylist>,
        val token: String? = null,
        val nextToken: String? = null,
    ) {
        val playlistIds: List<String>
            get() = playlists.map { it.id }
    }

    data class VideoSearchResult(
        val videos: ImmutableList<YoutubeVideo>,
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


    /** OPEN METHODS **************************************************************************************************/

    open fun getJson(videoId: String? = null): Map<String, Any?> {
        val map = mutableMapOf(
            "contentCheckOk" to true,
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to clientName,
                    "clientVersion" to clientVersion,
                    "gl" to region.name,
                    "hl" to "en_US",
                ),
                "request" to mapOf(
                    "internalExperimentFlags" to emptyList<Any>(),
                    "useSsl" to false,
                ),
            ),
            "playbackContext" to mapOf(
                "contentPlaybackContext" to mapOf(
                    "html5Preference" to "HTML5_PREF_WANTS",
                ),
            ),
            "racyCheckOk" to true,
            "thirdParty" to emptyMap<String, Any>(),
            "user" to mapOf(
                "lockedSafetyMode" to false,
            ),
        )

        if (videoId != null) map["videoId"] = videoId

        return map.toMap()
    }

    open fun getHeaders(videoId: String? = null): Map<String, String> {
        val map = mutableMapOf(
            "Accept" to "*/*",
            "Accept-Charset" to "ISO-8859-1,utf-8;q=0.7,*;q=0.7",
            "Accept-Encoding" to "gzip, deflate",
            "Accept-Language" to "en-US,en;q=0.5",
            "Origin" to "https://www.youtube.com",
        )
        if (videoId != null) map["Referer"] = "https://www.youtube.com/watch?v=$videoId"

        return map.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    open suspend fun getMetadata(videoId: String): List<YoutubeMetadata> {
        val response = postJson(url = PLAYER_URL, videoId = videoId)
        val formats =
            (response["streamingData"] as? Map<*, *>)?.get("formats") as? Collection<Map<*, *>> ?: emptyList()
        val adaptiveFormats =
            (response["streamingData"] as? Map<*, *>)?.get("adaptiveFormats") as? Collection<Map<*, *>>
                ?: emptyList()
        val metadataList = mutableListOf<YoutubeMetadata>()

        formats.plus(adaptiveFormats).forEach { fmt ->
            val mimeType = fmt["mimeType"] as? String
            val bitrate = fmt["bitrate"] as? Double
            val sampleRate = fmt["audioSampleRate"] as? String
            val url = fmt["url"] as? String
            val contentLength = fmt["contentLength"] as? String

            if (mimeType != null && bitrate != null && sampleRate != null && url != null && contentLength != null) {
                metadataList.add(
                    YoutubeMetadata(
                        mimeType = mimeType,
                        bitrate = bitrate.toInt(),
                        sampleRate = sampleRate.toInt(),
                        url = url,
                        size = contentLength.toInt(),
                        channels = (fmt["audioChannels"] as? Double)?.toInt(),
                        loudnessDb = fmt["loudnessDb"] as? Double,
                        durationMs = (fmt["approxDurationMs"] as? String)?.toLong(),
                    )
                )
            }
        }

        return metadataList.toList()
    }

    open fun getNextContinuationToken(response: Map<String, *>, isContinuationResponse: Boolean): String? {
        return if (!isContinuationResponse) response.yquery<Collection<Map<*, *>>>("contents.sectionListRenderer.continuations")
            ?.firstNotNullOfOrNull { it.yqueryString("nextContinuationData.continuation") }
        else response.yquery<Collection<Map<*, *>>>("continuationContents.sectionListContinuation.continuations")
            ?.firstNotNullOfOrNull { it.yqueryString("nextContinuationData.continuation") }
    }

    open fun getParams(videoId: String? = null): Map<String, String> = mapOf(
        "prettyPrint" to "false",
        "key" to key,
    )

    open fun getPlaylistVideoRenderers(response: Map<String, *>): Collection<Map<*, *>>? {
        return response.yquery<Collection<Map<*, *>>>("contents.singleColumnBrowseResultsRenderer.tabs")
            ?.flatMap {
                it.yquery<Collection<Map<*, *>>>("tabRenderer.content.sectionListRenderer.contents") ?: emptyList()
            }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("playlistVideoListRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("playlistVideoRenderer") }
    }

    open fun getPlaylistRenderers(response: Map<String, *>, isContinuationResponse: Boolean): Collection<Map<*, *>>? {
        return if (!isContinuationResponse)
            response.yquery<Collection<Map<*, *>>>("contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents")
                ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
                ?.mapNotNull { it.yquery<Map<*, *>>("playlistRenderer") }
        else response.yquery<Collection<Map<*, *>>>("onResponseReceivedCommands")
            ?.flatMap {
                it.yquery<Collection<Map<*, *>>>("appendContinuationItemsAction.continuationItems") ?: emptyList()
            }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("playlistRenderer") }
    }

    open fun getVideoRenderers(response: Map<String, *>, isContinuationResponse: Boolean): Collection<Map<*, *>>? {
        return if (!isContinuationResponse) response.yquery<Collection<Map<*, *>>>("contents.sectionListRenderer.contents")
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("compactVideoRenderer") }
        else response.yquery<Collection<Map<*, *>>>("continuationContents.sectionListContinuation.contents")
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("compactVideoRenderer") }
    }

    open suspend fun getVideoSearchResult(
        context: Context,
        query: String,
        continuationToken: String? = null,
    ): VideoSearchResult {
        val json =
            if (continuationToken != null) mapOf("continuation" to continuationToken)
            else mapOf("query" to query)
        val response = postJson(url = SEARCH_URL, json = json)
        val videoRenderers = getVideoRenderers(response, continuationToken != null)
        val nextContinuationToken = getNextContinuationToken(response, continuationToken != null)
        val videos = videoRenderers?.mapNotNull { loadVideoRenderer(it) } ?: emptyList()

        return VideoSearchResult(
            videos = videos.toImmutableList(),
            token = continuationToken,
            nextToken = nextContinuationToken,
        )
    }

    open suspend fun searchPlaylistCombos(
        context: Context,
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<YoutubePlaylistCombo> {
        val body = getStringResponse(url = RESULTS_URL, params = mapOf("search_query" to query))
        val ytDataJson = extractYtInitialData(body)
        val ytData = ytDataJson?.let { gson.fromJson(it, responseType) }

        if (ytData != null) {
            val renderers = ytData.yquery<Collection<Map<*, *>>>("contents.sectionListRenderer.contents")
                ?.mapNotNull { it.yquery<Map<*, *>>("universalWatchCardRenderer.header.watchCardRichHeaderRenderer") }
            val playlists = renderers?.mapNotNull { loadWatchCardRichHeaderRenderer(it) } ?: emptyList()
            val progressIncrement = 1.0 / (playlists.size + 1)

            progressCallback(progressIncrement)

            return playlists.mapIndexedNotNull { index, playlist ->
                getPlaylistComboFromPlaylistId(
                    playlistId = playlist.id,
                    artist = playlist.artist,
                ).also { progressCallback(progressIncrement * (index + 2)) }
            }
        }

        return emptyList()
    }


    /** PUBLIC METHODS ************************************************************************************************/

    suspend fun getNextPlaylistSearchResult(continuationToken: String): PlaylistSearchResult {
        val response = postJson(url = SEARCH_URL, json = mapOf("continuation" to continuationToken))
        val playlistRenderers = getPlaylistRenderers(response, true)
        val nextToken = getNextContinuationToken(response, true)

        return PlaylistSearchResult(
            playlists = playlistRenderers
                ?.mapNotNull { loadPlaylistRenderer(it) }
                ?.toImmutableList()
                ?: persistentListOf(),
            token = continuationToken,
            nextToken = nextToken,
        )
    }

    suspend fun getPlaylistSearchResult(query: String, playlistSpecificSearch: Boolean): PlaylistSearchResult {
        val params = mutableMapOf("search_query" to query)
        if (!playlistSpecificSearch) params["sp"] = "EgIQAw=="
        val body = getStringResponse(url = RESULTS_URL, params = params)
        val ytDataJson = extractYtInitialData(body)
        val ytData = ytDataJson?.let { gson.fromJson(it, responseType) }
        val playlists = mutableListOf<YoutubePlaylist>()
        var continuationToken: String? = null

        if (ytData != null) {
            val headerRenderers =
                ytData.yquery<Collection<Map<*, *>>>("contents.twoColumnSearchResultsRenderer.secondaryContents.secondarySearchContainerRenderer.contents")
                    ?.mapNotNull { it.yquery<Map<*, *>>("universalWatchCardRenderer.header.watchCardRichHeaderRenderer") }
            val playlistRenderers = getPlaylistRenderers(ytData, false)

            continuationToken = getNextContinuationToken(ytData, false)
            headerRenderers
                ?.mapNotNull { loadWatchCardRichHeaderRenderer(it) }
                ?.mapNotNull { getPlaylistFromPlaylistId(playlistId = it.id, artist = it.artist) }
                ?.also { playlists.addAll(it) }
            playlistRenderers
                ?.mapNotNull { loadPlaylistRenderer(it) }
                ?.filter { playlist -> !playlists.map { it.id }.contains(playlist.id) }
                ?.also { playlists.addAll(it) }
        }

        return PlaylistSearchResult(
            playlists = playlists.toImmutableList(),
            nextToken = continuationToken,
        )
    }

    suspend fun postJson(
        url: String,
        videoId: String? = null,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        json: Map<String, Any?> = emptyMap(),
    ): Map<String, *> {
        return Request.postJson(
            url = url,
            params = getParams(videoId).plus(params),
            headers = getHeaders(videoId).plus(headers),
            json = getJson(videoId).mergeWith(json),
        ).getJson()
    }


    /** PROTECTED METHODS *********************************************************************************************/

    protected fun getImageData(thumbnails: Collection<Map<*, *>>?): ImageData? {
        /**
         * Extracts image URLs from API response, works for both playlists and videos.
         * Does not fetch the actual images.
         */
        val thumbnailSizePx = IMAGE_THUMBNAIL_MAX_WIDTH_PX * IMAGE_THUMBNAIL_MAX_WIDTH_PX
        val images = mutableListOf<YoutubeImage>()

        thumbnails?.forEach { thumbnail ->
            val tnUrl = thumbnail["url"] as? String
            val tnWidth = thumbnail["width"] as? Double
            val tnHeight = thumbnail["height"] as? Double
            if (tnUrl != null && tnWidth != null && tnHeight != null) {
                images.add(YoutubeImage(url = tnUrl, width = tnWidth.toInt(), height = tnHeight.toInt()))
            }
        }

        return if (images.isNotEmpty()) ImageData(
            thumbnail = images
                .sortedBy { (it.shortestSide - thumbnailSizePx).absoluteValue }
                .minByOrNull { it.shortestSide < thumbnailSizePx },
            fullImage = images.maxByOrNull { it.shortestSide },
        )
        else null
    }

    suspend fun getPlaylistComboFromPlaylistId(playlistId: String, artist: String? = null): YoutubePlaylistCombo? {
        val response = postJson(url = BROWSE_URL, json = mapOf("browseId" to "VL$playlistId"))
        val playlistVideoRenderers = getPlaylistVideoRenderers(response)
        val playlist = getPlaylistFromBrowseResponse(
            playlistId = playlistId,
            response = response,
            artist = artist,
        )

        if (playlist != null) {
            val videos = mutableListOf<YoutubeVideo>()

            playlistVideoRenderers?.forEach { renderer ->
                val videoId = renderer.yqueryString("videoId")
                val videoTitle = renderer.yqueryString("title")
                val lengthSeconds = renderer.yqueryString("lengthSeconds")
                val videoImageData = getImageData(
                    thumbnails = renderer.yquery<Collection<Map<*, *>>>("thumbnail.thumbnails"),
                )

                if (videoId != null && videoTitle != null) {
                    videos.add(
                        YoutubeVideo(
                            id = videoId,
                            title = videoTitle,
                            durationMs = lengthSeconds?.toLong()?.times(1000),
                            thumbnail = videoImageData?.thumbnail,
                            fullImage = videoImageData?.fullImage,
                        )
                    )
                }
            }

            return YoutubePlaylistCombo(playlist = playlist, videos = videos.toImmutableList())
        }

        return null
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun extractYtInitialData(body: String): String? {
        val innerRegex = Regex("\\\\x(..)")

        Regex("var ytInitialData *= *(\\{.*?\\});", RegexOption.MULTILINE)
            .find(body)
            ?.groupValues
            ?.lastOrNull()
            ?.also { return it }

        Regex("var ytInitialData *= *'(\\\\x7b.*?\\\\x7d)'", RegexOption.MULTILINE)
            .find(body)
            ?.groupValues
            ?.lastOrNull()
            ?.replace(innerRegex) { result ->
                result.groupValues.last().toByte(16).toInt().toChar().toString()
            }
            ?.also { return it }

        return null
    }

    private fun getPlaylistFromBrowseResponse(
        playlistId: String,
        response: Map<String, *>,
        artist: String? = null,
    ): YoutubePlaylist? {
        val title = response.yqueryString("metadata.playlistMetadataRenderer.albumName") // only on web
            ?: response.yqueryString("header.playlistHeaderRenderer.title")
        val videoCount = response.yqueryString("header.playlistHeaderRenderer.numVideosText")
        val playlistArtist = response.yqueryString("header.playlistHeaderRenderer.subtitle")
            ?.substringBeforeLast(" • Album")  // only on web

        if (title != null) {
            val imageData = getImageData(
                thumbnails = response.yquery<Collection<Map<*, *>>>(
                    "header.playlistHeaderRenderer.playlistHeaderBanner.heroPlaylistThumbnailRenderer.thumbnail.thumbnails"
                ),
            )

            return YoutubePlaylist(
                id = playlistId,
                title = title,
                artist = artist ?: playlistArtist,
                videoCount = videoCount?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0,
                thumbnail = imageData?.thumbnail,
                fullImage = imageData?.fullImage,
            )
        }

        return null
    }

    private suspend fun getPlaylistFromPlaylistId(playlistId: String, artist: String? = null): YoutubePlaylist? =
        getPlaylistFromBrowseResponse(
            playlistId = playlistId,
            response = postJson(url = BROWSE_URL, json = mapOf("browseId" to "VL$playlistId")),
            artist = artist,
        )

    private suspend fun getStringResponse(
        url: String,
        videoId: String? = null,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): String {
        return Request(
            url = url,
            params = getParams(videoId).plus(params),
            headers = getHeaders(videoId).plus(headers),
        ).getString()
    }

    private fun loadPlaylistRenderer(renderer: Map<*, *>): YoutubePlaylist? {
        val playlistId = renderer.yqueryString("playlistId")
        val title = renderer.yqueryString("title")
        val videoCount = renderer.yqueryString("videoCount") ?: renderer.yqueryString("videoCountText")
        val thumbnails = mutableListOf<Map<*, *>>()

        renderer.yquery<Collection<Map<*, *>>>("thumbnails")
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("thumbnails") ?: emptyList() }
            ?.also { thumbnails.addAll(it) }
        renderer.yquery<Collection<Map<*, *>>>("thumbnailRenderer.playlistVideoThumbnailRenderer.thumbnail.thumbnails")
            ?.also { thumbnails.addAll(it) }

        val imageData = getImageData(thumbnails = thumbnails)

        if (title != null && playlistId != null) {
            return YoutubePlaylist(
                id = playlistId,
                title = title,
                thumbnail = imageData?.thumbnail,
                fullImage = imageData?.fullImage,
                videoCount = videoCount?.toInt() ?: 0,
            )
        }
        return null
    }

    private fun loadVideoRenderer(videoRenderer: Map<*, *>): YoutubeVideo? {
        val videoId = videoRenderer.yqueryString("videoId")
        val title = videoRenderer.yqueryString("title")
        val imageData = getImageData(
            thumbnails = videoRenderer.yquery<Collection<Map<*, *>>>("thumbnail.thumbnails"),
        )
        val lengthText = videoRenderer.yqueryString("lengthText")
        val durationMs = lengthText?.toDuration()?.inWholeMilliseconds

        if (videoId != null && title != null) {
            return YoutubeVideo(
                id = videoId,
                title = title,
                thumbnail = imageData?.thumbnail,
                fullImage = imageData?.fullImage,
                durationMs = durationMs,
            )
        }
        return null
    }

    private fun loadWatchCardRichHeaderRenderer(renderer: Map<*, *>): YoutubePlaylist? {
        val listTitle = renderer.yqueryString("title")
        val artist = renderer.yqueryString("subtitle")
            ?.takeIf { it.contains("Album • ") }
            ?.substringAfter("Album • ")
        val playlistId = renderer.yqueryString("titleNavigationEndpoint.commandMetadata.webCommandMetadata.url")
            ?.split("=")
            ?.last()

        if (listTitle != null && playlistId != null) {
            val title = artist?.let { listTitle.replace(Regex(Pattern.quote("^$it - ")), "") } ?: listTitle

            return YoutubePlaylist(artist = artist, title = title, id = playlistId)
        }
        return null
    }

    companion object {
        const val BROWSE_URL = "https://www.youtube.com/youtubei/v1/browse"
        const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
        const val RESULTS_URL = "https://www.youtube.com/results"
        const val SEARCH_URL = "https://www.youtube.com/youtubei/v1/search"

        val gson: Gson = GsonBuilder().create()
        val responseType = object : TypeToken<Map<String, *>>() {}
    }
}


abstract class AbstractYoutubeAndroidClient(region: Region = Region.SE) : AbstractYoutubeClient(region = region) {
    abstract val id: String
    abstract val osVersion: String

    open val osName: String = "Android"
    open val userAgent: String = "com.google.android.youtube"

    /** OVERRIDDEN METHODS ****************************************************/
    override fun getHeaders(videoId: String?): Map<String, String> = super.getHeaders(videoId).plus(
        mapOf(
            "User-Agent" to "$userAgent/$clientVersion (Linux; U; $osName $osVersion; ${region.name}) gzip",
            "X-YouTube-Client-Name" to id,
            "X-YouTube-Client-Version" to clientVersion,
        )
    )

    override fun getJson(videoId: String?): Map<String, Any?> {
        return super.getJson(videoId).mergeWith(
            mapOf(
                "context" to mapOf(
                    "client" to mapOf(
                        "androidSdkVersion" to "34",
                        "osName" to osName,
                        "osVersion" to osVersion,
                        "platform" to "MOBILE",
                    ),
                ),
                "params" to "2AMBCgIQBg",
            )
        )
    }
}


abstract class AbstractYoutubeAndroidNonStandardClient(region: Region = Region.SE) :
    AbstractYoutubeAndroidClient(region) {
    override fun getNextContinuationToken(response: Map<String, *>, isContinuationResponse: Boolean): String? {
        return if (!isContinuationResponse) response.yquery<Collection<Map<*, *>>>("contents.sectionListRenderer.contents")
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.continuations") ?: emptyList() }
            ?.firstNotNullOfOrNull { it.yqueryString("nextContinuationData.continuation") }
        else response.yquery<Collection<Map<*, *>>>("continuationContents.itemSectionContinuation.continuations")
            ?.firstNotNullOfOrNull { it.yqueryString("nextContinuationData.continuation") }
    }

    override fun getVideoRenderers(response: Map<String, *>, isContinuationResponse: Boolean): Collection<Map<*, *>>? {
        return if (!isContinuationResponse) super.getVideoRenderers(response, false)
        else response.yquery<Collection<Map<*, *>>>("continuationContents.itemSectionContinuation.contents")
            ?.mapNotNull { it.yquery<Map<*, *>>("compactVideoRenderer") }
    }
}


class YoutubeAndroidTestSuiteClient(region: Region = Region.SE) : AbstractYoutubeAndroidNonStandardClient(region) {
    override val clientName = "ANDROID_TESTSUITE"
    override val clientVersion = "1.9"
    override val id = "30"
    override val key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
    override val osVersion = "14"

    override suspend fun searchPlaylistCombos(
        context: Context,
        query: String,
        progressCallback: (Double) -> Unit,
    ): List<YoutubePlaylistCombo> {
        throw Exception("2024-04-02: getPlaylistComboFromPlaylist() returns HTTP 500 for this client.")
    }
}


class YoutubeAndroidClient(region: Region = Region.SE) : AbstractYoutubeAndroidClient(region = region) {
    override val clientName = "ANDROID"
    override val clientVersion = "19.12.36"
    override val id = "3"
    override val key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
    override val osVersion = "14"

    override fun getVideoRenderers(response: Map<String, *>, isContinuationResponse: Boolean): Collection<Map<*, *>>? {
        return if (isContinuationResponse)
            response.yquery<Collection<Map<*, *>>>("continuationContents.sectionListContinuation.contents")
                ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
                ?.mapNotNull { it.yquery<Map<*, *>>("compactVideoRenderer") }
        else super.getVideoRenderers(response, false)
    }

    override suspend fun getMetadata(videoId: String): List<YoutubeMetadata> {
        throw Exception("2024-04-02: Only returns URLs to 5 minute tracks of silence.")
    }
}


class YoutubeAndroidEmbeddedClient(region: Region = Region.SE) : AbstractYoutubeAndroidNonStandardClient(region) {
    override val clientName = "ANDROID_EMBEDDED_PLAYER"
    override val clientVersion = "19.12.36"
    override val id = "55"
    override val key = "AIzaSyCjc_pVEDi4qsv5MtC2dMXzpIaDoRFLsxw"
    override val osVersion = "14"

    override fun getJson(videoId: String?): Map<String, Any?> {
        val map = super.getJson(videoId).mergeWith(
            mapOf("context" to mapOf("client" to mapOf("clientScreen" to "EMBED"))),
        ).toMutableMap()

        if (videoId != null) map["thirdParty"] = mapOf("embedUrl" to "https://www.youtube.com/embed/$videoId")

        return map.toMap()
    }
}


class YoutubeAndroidUnpluggedClient(region: Region = Region.SE) : AbstractYoutubeAndroidNonStandardClient(region) {
    override val clientName = "ANDROID_UNPLUGGED"
    override val clientVersion = "8.12.0"
    override val id = "29"
    override val key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
    override val osVersion = "14"
    override val userAgent = "com.google.android.apps.youtube.unplugged"

    override suspend fun getMetadata(videoId: String): List<YoutubeMetadata> {
        throw Exception("2024-04-02: Returns no entries for this client.")
    }

    override suspend fun getVideoSearchResult(
        context: Context,
        query: String,
        continuationToken: String?,
    ): VideoSearchResult {
        throw Exception("2024-04-02: Returns HTTP 401 for this client.")
    }

    override suspend fun searchPlaylistCombos(
        context: Context,
        query: String,
        progressCallback: (Double) -> Unit,
    ): List<YoutubePlaylistCombo> {
        throw Exception("2024-04-02: getPlaylistComboFromPlaylist() returns HTTP 500 for this client.")
    }
}


class YoutubeIOSClient(region: Region = Region.SE) : AbstractYoutubeClient(region = region) {
    private val build = "21E236"
    private val deviceModel = "iPhone16,2"
    private val major = "17"
    private val minor = "4"
    private val osName = "iOS"
    private val patch = "1"

    override val clientName = "IOS"
    override val clientVersion = "19.12.3"
    override val key = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc"

    override fun getHeaders(videoId: String?): Map<String, String> = super.getHeaders(videoId).plus(
        mapOf(
            "User-Agent" to "com.google.ios.youtube/$clientVersion ($deviceModel; U; CPU $osName ${major}_${minor}_$patch like Mac OS X)",
            "X-YouTube-Client-Name" to "5",
            "X-YouTube-Client-Version" to clientVersion,
        )
    )

    override fun getJson(videoId: String?): Map<String, Any?> = super.getJson(videoId).mergeWith(
        mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "androidSdkVersion" to "34",
                    "deviceModel" to deviceModel,
                    "osName" to osName,
                    "osVersion" to "$major.$minor.$patch.$build",
                    "platform" to "MOBILE",
                ),
            ),
            "params" to "2AMBCgIQBg",
        )
    )

    override fun getPlaylistVideoRenderers(response: Map<String, *>): Collection<Map<*, *>>? {
        return response.yquery<Collection<Map<*, *>>>("contents.singleColumnBrowseResultsRenderer.tabs")
            ?.flatMap {
                it.yquery<Collection<Map<*, *>>>("tabRenderer.content.sectionListRenderer.contents") ?: emptyList()
            }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("playlistVideoListRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("playlistVideoRenderer") }
    }

    override suspend fun getVideoSearchResult(
        context: Context,
        query: String,
        continuationToken: String?,
    ): VideoSearchResult {
        val json =
            if (continuationToken != null) mapOf("continuation" to continuationToken)
            else mapOf("query" to query)
        val response = postJson(url = SEARCH_URL, json = json)
        val videos = mutableListOf<YoutubeVideo>()
        val videoDataList = getVideoDataList(response, continuationToken != null)
        val nextContinuationToken = getNextContinuationToken(response, continuationToken != null)

        videoDataList?.forEach { videoData ->
            val title = videoData.yqueryString("videoData.metadata.title")
            val imageData = getImageData(
                thumbnails = videoData.yquery<Collection<Map<*, *>>>("videoData.thumbnail.image.sources"),
            )
            val durationMs = videoData.yqueryString("accessibilityText")
                ?.replace(Regex("^.*(\\d+) minutes, (\\d+) seconds.*$"), "$1:$2")
                ?.toDuration()
                ?.inWholeMilliseconds
            val videoId = videoData.yqueryString("videoData.dragAndDropUrl")?.substringAfterLast('=')

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

        return VideoSearchResult(
            videos = videos.toImmutableList(),
            token = continuationToken,
            nextToken = nextContinuationToken
        )
    }

    private fun getVideoDataList(response: Map<String, *>, isContinuationResponse: Boolean): Collection<Map<*, *>>? {
        return if (!isContinuationResponse) response.yquery<Collection<Map<*, *>>>("contents.sectionListRenderer.contents")
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("elementRenderer.newElement.type.componentType.model.compactVideoModel.compactVideoData") }
        else response.yquery<Collection<Map<*, *>>>("continuationContents.sectionListContinuation.contents")
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("elementRenderer.newElement.type.componentType.model.compactVideoModel.compactVideoData") }
    }
}


class YoutubeWebClient(region: Region = Region.SE) : AbstractYoutubeClient(region = region) {
    override val clientName = "WEB"
    override val clientVersion = "2.20240304.00.00"
    override val key = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    override fun getHeaders(videoId: String?): Map<String, String> {
        val map = super.getHeaders(videoId).plus(
            mapOf(
                "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0",
                "Host" to "www.youtube.com",
            ),
        ).toMutableMap()

        if (videoId != null) map["Referer"] = "https://www.youtube.com/watch?v=$videoId"

        return map.toMap()
    }

    override suspend fun getMetadata(videoId: String): List<YoutubeMetadata> {
        throw Exception("2024-04-02: Returns no URLs for this client.")
    }

    override fun getNextContinuationToken(response: Map<String, *>, isContinuationResponse: Boolean): String? {
        return if (!isContinuationResponse)
            response.yquery<Collection<Map<*, *>>>("contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents")
                ?.firstNotNullOfOrNull { it.yqueryString("continuationItemRenderer.continuationEndpoint.continuationCommand.token") }
        else response.yquery<Collection<Map<*, *>>>("onResponseReceivedCommands")
            ?.flatMap {
                it.yquery<Collection<Map<*, *>>>("appendContinuationItemsAction.continuationItems") ?: emptyList()
            }
            ?.firstNotNullOfOrNull { it.yqueryString("continuationItemRenderer.continuationEndpoint.continuationCommand.token") }
    }

    override fun getPlaylistVideoRenderers(response: Map<String, *>): Collection<Map<*, *>>? {
        return response.yquery<Collection<Map<*, *>>>("contents.twoColumnBrowseResultsRenderer.tabs")
            ?.flatMap {
                it.yquery<Collection<Map<*, *>>>("tabRenderer.content.sectionListRenderer.contents") ?: emptyList()
            }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("playlistVideoListRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("playlistVideoRenderer") }
    }

    override suspend fun searchPlaylistCombos(
        context: Context,
        query: String,
        progressCallback: (Double) -> Unit,
    ): List<YoutubePlaylistCombo> {
        /**
         * First do a default search, which hopefully will bring us an album section in the right side column (i.e.
         * contents.twoColumnSearchResultsRenderer.secondaryContents). Then continue on with GET param sp="EgIQAw==",
         * which for some reason is the way to filter for playlists.
         */
        val initialResult = getPlaylistSearchResult(query = query, playlistSpecificSearch = true)
        val secondaryResult = getPlaylistSearchResult(query = query, playlistSpecificSearch = false)
        val playlists =
            initialResult.playlists.plus(secondaryResult.playlists.filter { !initialResult.playlistIds.contains(it.id) })
        val progressIncrement = 1.0 / (playlists.size + 1)

        progressCallback(progressIncrement)

        return playlists.mapIndexedNotNull { index, playlist ->
            getPlaylistComboFromPlaylistId(
                playlistId = playlist.id,
                artist = playlist.artist
            ).also { progressCallback(progressIncrement * (index + 2)) }
        }
    }

    override fun getVideoRenderers(response: Map<String, *>, isContinuationResponse: Boolean): Collection<Map<*, *>>? {
        return if (!isContinuationResponse)
            response.yquery<Collection<Map<*, *>>>("contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents")
                ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
                ?.mapNotNull { it.yquery<Map<*, *>>("videoRenderer") }
        else response.yquery<Collection<Map<*, *>>>("onResponseReceivedCommands")
            ?.flatMap {
                it.yquery<Collection<Map<*, *>>>("appendContinuationItemsAction.continuationItems") ?: emptyList()
            }
            ?.flatMap { it.yquery<Collection<Map<*, *>>>("itemSectionRenderer.contents") ?: emptyList() }
            ?.mapNotNull { it.yquery<Map<*, *>>("videoRenderer") }
    }
}
