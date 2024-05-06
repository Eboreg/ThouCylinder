package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.stripCommonFixes
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.interfaces.IExternalTrack
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
@Immutable
data class YoutubeVideo(
    override val id: String,
    override val title: String,
    val durationMs: Long? = null,
    val artist: String? = null,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
) : Parcelable, IExternalTrack {
    data class TrackMatch(
        val distance: Int,
        val video: YoutubeVideo,
    )

    val duration: Duration?
        get() = metadata?.durationMs?.milliseconds ?: durationMs?.milliseconds

    val metadataRefreshNeeded: Boolean
        get() = metadata == null || metadata.urlIsOld || metadata.lofiUrlIsOld

    fun matchTrack(
        track: Track,
        albumArtists: Collection<AbstractArtistCredit>? = null,
        trackArtists: Collection<AbstractArtistCredit>? = null,
    ) = TrackMatch(
        distance = getTrackDistance(
            track = track,
            albumArtists = albumArtists,
            trackArtists = trackArtists,
        ),
        video = this,
    )

    fun toTrack(
        isInLibrary: Boolean,
        artistName: String? = null,
        album: Album? = null,
        albumPosition: Int? = null,
    ) = Track(
        title = artistName
            ?.let { title.replace(Regex("^$it (- )?", RegexOption.IGNORE_CASE), "") }
            ?: title,
        isInLibrary = isInLibrary,
        albumId = album?.albumId,
        albumPosition = albumPosition,
        youtubeVideo = this,
        durationMs = metadata?.durationMs ?: durationMs,
        image = fullImage?.let {
            MediaStoreImage(
                fullUriString = it.url,
                thumbnailUriString = thumbnail?.url ?: it.url,
            )
        },
    )

    fun toTrackCombo(
        isInLibrary: Boolean,
        albumArtist: Artist? = null,
        album: Album? = null,
        albumPosition: Int? = null,
    ): TrackCombo {
        val track = toTrack(
            isInLibrary = isInLibrary,
            artistName = albumArtist?.name,
            album = album,
            albumPosition = albumPosition,
        )

        return TrackCombo(
            track = track,
            album = album,
            artists = albumArtist?.let { listOf(TrackArtistCredit(artist = it, trackId = track.trackId)) }
                ?: emptyList(),
        )
    }

    private fun getTrackDistance(
        track: Track,
        albumArtists: Collection<AbstractArtistCredit>? = null,
        trackArtists: Collection<AbstractArtistCredit>? = null,
    ): Int {
        val levenshtein = LevenshteinDistance()
        val titleDistances = mutableListOf<Int>()
        var distance = 0
        val artists = mutableListOf<AbstractArtistCredit>()

        trackArtists?.also { artists.addAll(it) }
        albumArtists?.also { artists.addAll(it) }

        // Test various permutations of "[artist] - [title]":
        titleDistances.add(levenshtein.apply(track.title.lowercase(), title.lowercase()))
        trackArtists?.joined()?.also {
            titleDistances.add(levenshtein.apply("$it - ${track.title}".lowercase(), title.lowercase()))
        }
        artists.forEach {
            titleDistances.add(levenshtein.apply("${it.name} - ${track.title}".lowercase(), title.lowercase()))
        }
        distance += titleDistances.min()

        // Add number of seconds diffing:
        duration?.inWholeSeconds
            ?.let { track.duration?.inWholeSeconds?.minus(it) }
            ?.also { distance += it.toInt().absoluteValue }

        return distance
    }
}

fun Iterable<YoutubeVideo>.stripTitleCommons(): List<YoutubeVideo> = zip(map { it.title }.stripCommonFixes())
    .map { (video, title) -> video.copy(title = title.replace(Regex(" \\([^)]*$"), "")) }
