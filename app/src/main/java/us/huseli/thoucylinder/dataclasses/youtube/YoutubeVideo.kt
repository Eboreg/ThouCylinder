package us.huseli.thoucylinder.dataclasses.youtube

import android.os.Parcelable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.stripCommonFixes
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtistCredit
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.abstr.toArtists
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.joined
import us.huseli.thoucylinder.interfaces.IExternalTrack
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class YoutubeVideo(
    override val id: String,
    override val title: String,
    val durationMs: Long? = null,
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
        get() = metadata == null || metadata.isOld

    fun matchTrack(
        track: Track,
        albumArtists: List<Artist>? = null,
        albumArtist: String? = albumArtists?.joined(),
        trackArtists: List<AbstractArtistCredit>? = null,
    ) = TrackMatch(
        distance = getTrackDistance(
            track = track,
            albumArtists = albumArtists,
            albumArtist = albumArtist,
            trackArtists = trackArtists,
        ),
        video = this,
    )

    fun toTrack(isInLibrary: Boolean, artist: String? = null, album: Album? = null, albumPosition: Int? = null) = Track(
        title = artist?.let { title.replace(Regex("^$it (- )?", RegexOption.IGNORE_CASE), "") } ?: title,
        isInLibrary = isInLibrary,
        albumId = album?.albumId,
        albumPosition = albumPosition,
        youtubeVideo = this,
        durationMs = metadata?.durationMs ?: durationMs,
    )

    fun toTrackCombo(isInLibrary: Boolean, artist: String? = null, album: Album? = null, albumPosition: Int? = null) =
        TrackCombo(
            track = toTrack(isInLibrary = isInLibrary, artist = artist, album = album, albumPosition = albumPosition),
            album = album,
            artists = emptyList(),
        )

    private fun getTrackDistance(
        track: Track,
        albumArtists: List<Artist>? = null,
        albumArtist: String? = albumArtists?.joined(),
        trackArtists: List<AbstractArtistCredit>? = null,
    ): Int {
        val levenshtein = LevenshteinDistance()
        val titleDistances = mutableListOf<Int>()
        var distance = 0
        val artists = mutableListOf<Artist>()

        trackArtists?.also { artists.addAll(it.toArtists()) }
        (albumArtists ?: albumArtist?.let { listOf(Artist(name = it)) })?.also { artists.addAll(it) }

        // Test various permutations of "[artist] - [title]":
        titleDistances.add(levenshtein.apply(track.title.lowercase(), title.lowercase()))
        trackArtists?.joined()?.also {
            titleDistances.add(levenshtein.apply("$it - ${track.title}".lowercase(), title.lowercase()))
        }
        albumArtist?.also {
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
