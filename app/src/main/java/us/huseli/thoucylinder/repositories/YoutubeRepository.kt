package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.AbstractYoutubeClient
import us.huseli.thoucylinder.Constants.PREF_REGION
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.YoutubeAndroidClient
import us.huseli.thoucylinder.YoutubeAndroidTestSuiteClient
import us.huseli.thoucylinder.YoutubeWebClient
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeMetadata
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylistCombo
import us.huseli.thoucylinder.dataclasses.youtube.YoutubeVideo
import us.huseli.thoucylinder.dataclasses.youtube.getBest
import us.huseli.thoucylinder.enums.Region
import us.huseli.thoucylinder.getMutexCache
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener, AbstractScopeHolder() {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _region = MutableStateFlow(getRegion())

    val region = _region.asStateFlow()

    private val metadataCache = getMutexCache("YoutubeRepository.metadataCache") { videoId ->
        onIOThread { YoutubeAndroidTestSuiteClient(_region.value).getMetadata(videoId) }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    suspend fun downloadVideo(video: YoutubeVideo, progressCallback: (Double) -> Unit): File {
        return onIOThread {
            val metadata = getBestMetadata(video.id)
                ?: throw Exception("Could not get Youtube metadata for video ${video.title} (id=${video.id})")
            val tempFile = File(context.cacheDir, "${UUID.randomUUID()}.${metadata.fileExtension}")

            tempFile.outputStream().use { outputStream ->
                var rangeStart = 0
                var finished = false
                var contentLength: Int? = null

                while (!finished) {
                    val request = Request(
                        url = metadata.url,
                        headers = mapOf("Range" to "bytes=$rangeStart-${DOWNLOAD_CHUNK_SIZE + rangeStart}"),
                    )

                    request.getInputStream().use { inputStream ->
                        if (contentLength == null) contentLength = request.contentLength
                        outputStream.write(inputStream.readBytes())
                        contentLength?.also {
                            progressCallback(((DOWNLOAD_CHUNK_SIZE + rangeStart).toDouble() / it).coerceAtMost(1.0))
                        }
                        request.contentRange.also {
                            if (it?.size != null && it.size - it.rangeEnd > 1) rangeStart = it.rangeEnd + 1
                            else finished = true
                        }
                    }
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
            if (forceReload || track.youtubeVideo?.metadataRefreshNeeded == true)
                track.youtubeVideo?.id?.let { getBestMetadata(videoId = it, forceReload = forceReload) }
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
        albumArtists: Collection<IArtistCredit>? = null,
        trackArtists: Collection<IArtistCredit>? = null,
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
        combo: IAlbumWithTracksCombo<*>,
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

    suspend fun getBestTrackMatch(
        track: Track,
        albumArtists: Collection<IArtistCredit>? = null,
        trackArtists: Collection<IArtistCredit>? = null,
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


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun ensureTrackMetadataOrNull(track: Track): Track? = ensureTrackMetadata(track = track)
        .takeIf { it.metadata != null && it.youtubeVideo?.metadata != null }

    suspend fun getBestMetadata(videoId: String, forceReload: Boolean = false): YoutubeMetadata? =
        metadataCache.getOrNull(videoId, retryOnNull = true, forceReload = forceReload)?.getBest()

    private fun getRegion() =
        preferences.getString(PREF_REGION, null)?.let { Region.valueOf(it) } ?: Region.SE

    private suspend fun getVideoSearchResult(
        query: String,
        continuationToken: String? = null,
    ): AbstractYoutubeClient.VideoSearchResult = onIOThread {
        YoutubeAndroidClient(_region.value).getVideoSearchResult(query = query, continuationToken = continuationToken)
    }

    private suspend fun searchPlaylistCombos(
        query: String,
        progressCallback: (Double) -> Unit = {},
    ): List<YoutubePlaylistCombo> = onIOThread {
        YoutubeWebClient(_region.value).searchPlaylistCombos(query = query, progressCallback = progressCallback)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_REGION) _region.value = getRegion()
    }

    companion object {
        const val DOWNLOAD_CHUNK_SIZE = 10 shl 16
    }
}
