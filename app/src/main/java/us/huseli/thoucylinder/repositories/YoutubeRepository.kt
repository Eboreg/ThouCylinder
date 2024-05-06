package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.AbstractYoutubeClient
import us.huseli.thoucylinder.Constants.PREF_REGION
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.YoutubeAndroidClient
import us.huseli.thoucylinder.YoutubeAndroidTestSuiteClient
import us.huseli.thoucylinder.YoutubeTrackSearchMediator
import us.huseli.thoucylinder.YoutubeWebClient
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.YoutubePlaylistCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.parseContentRange
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.youtube.getBest
import us.huseli.thoucylinder.enums.Region
import us.huseli.thoucylinder.getMutexCache
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class YoutubeRepository @Inject constructor(
    private val database: Database,
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener, AbstractScopeHolder() {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val _isSearchingTracks = MutableStateFlow(false)
    private val _region = MutableStateFlow(getRegion())
    private val albumDao = database.albumDao()
    private val youtubeSearchDao = database.youtubeSearchDao()

    val importedPlaylistIds: StateFlow<List<String>> =
        albumDao.flowYoutubePlaylistIds().distinctUntilChanged().stateLazily(emptyList())
    val isSearchingTracks = _isSearchingTracks.asStateFlow()
    val region = _region.asStateFlow()

    private val metadataCache = getMutexCache("YoutubeRepository.metadataCache") { videoId ->
        YoutubeAndroidTestSuiteClient(_region.value).getMetadata(videoId)
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        launchOnIOThread { youtubeSearchDao.clearCache() }
    }

    suspend fun downloadVideo(video: YoutubeVideo, progressCallback: (Double) -> Unit): File {
        return withContext(Dispatchers.IO) {
            val metadata = getBestMetadata(video)
                ?: throw Exception("Could not get Youtube metadata for video ${video.title} (id=${video.id})")
            val tempFile = File(context.cacheDir, "${UUID.randomUUID()}.${metadata.fileExtension}")

            tempFile.outputStream().use { outputStream ->
                var rangeStart = 0
                var finished = false
                var contentLength: Int? = null

                while (!finished) {
                    val conn = Request(
                        url = metadata.url,
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
            tempFile
        }
    }

    suspend fun ensureTrackMetadata(
        track: Track,
        forceReload: Boolean = false,
        onChanged: suspend (Track) -> Unit = {},
    ): Track {
        val youtubeMetadata =
            if (forceReload || track.youtubeVideo?.metadataRefreshNeeded == true) getBestMetadata(track, forceReload)
            else track.youtubeVideo?.metadata
        val metadata =
            if (track.metadata == null || forceReload) youtubeMetadata?.toTrackMetadata()
            else track.metadata
        val changed = youtubeMetadata != track.youtubeVideo?.metadata || metadata != track.metadata
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

    suspend fun ensureTrackPlayUri(
        track: Track,
        albumArtists: Collection<AbstractArtistCredit>? = null,
        trackArtists: Collection<AbstractArtistCredit>? = null,
        onChanged: suspend (Track) -> Unit = {},
    ): Track {
        return track.takeIf { it.playUri != null }
            ?: track.takeIf { it.youtubeVideo != null }?.let { ensureTrackMetadata(it, onChanged = onChanged) }
            ?: getBestTrackMatch(
                track = track,
                albumArtists = albumArtists,
                trackArtists = trackArtists,
            )?.also { onChanged(it) } ?: track
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
        track.youtubeVideo?.let { getBestMetadata(it, forceReload = forceReload) }

    suspend fun getBestTrackMatch(trackCombo: TrackCombo, maxDistance: Int = 5): TrackCombo? = getBestTrackMatch(
        track = trackCombo.track,
        albumArtists = trackCombo.albumArtists,
        trackArtists = trackCombo.artists,
        maxDistance = maxDistance,
    )?.let { track -> trackCombo.copy(track = track) }

    suspend fun getVideoSearchResult(
        query: String,
        continuationToken: String? = null,
    ): AbstractYoutubeClient.VideoSearchResult = YoutubeAndroidClient(_region.value).getVideoSearchResult(
        context = context,
        query = query,
        continuationToken = continuationToken,
    )

    suspend fun searchPlaylistCombos(
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<YoutubePlaylistCombo> = YoutubeWebClient(_region.value).searchPlaylistCombos(
        context = context,
        query = query,
        progressCallback = progressCallback,
    )

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
    private suspend fun ensureTrackMetadataOrNull(track: Track): Track? = ensureTrackMetadata(track = track)
        .takeIf { it.metadata != null && it.youtubeVideo?.metadata != null }

    private suspend fun getBestMetadata(video: YoutubeVideo, forceReload: Boolean = false): YoutubeMetadata? =
        metadataCache.getOrNull(video.id, retryOnNull = true, forceReload = forceReload)?.getBest()

    private suspend fun getBestTrackMatch(
        track: Track,
        albumArtists: Collection<AbstractArtistCredit>? = null,
        trackArtists: Collection<AbstractArtistCredit>? = null,
        maxDistance: Int = 5,
    ): Track? {
        val artistString = trackArtists?.joined()
        val query = artistString?.let { "$it ${track.title}" } ?: track.title
        val videos = getVideoSearchResult(query).videos

        return videos
            .map {
                it.matchTrack(
                    track = track,
                    albumArtists = albumArtists,
                    trackArtists = trackArtists,
                )
            }
            .filter { it.distance <= maxDistance }
            .minByOrNull { it.distance }
            ?.let { ensureTrackMetadataOrNull(track.copy(youtubeVideo = it.video)) }
    }

    private fun getRegion() =
        preferences.getString(PREF_REGION, null)?.let { Region.valueOf(it) } ?: Region.SE

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_REGION) _region.value = getRegion()
    }

    companion object {
        const val DOWNLOAD_CHUNK_SIZE = 10 shl 16
    }
}
