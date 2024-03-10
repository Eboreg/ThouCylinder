package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.dataclasses.abstr.BaseArtist
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.interfaces.IExternalTrack
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractSpotifyTrack<AT : AbstractSpotifyArtist> : AbstractSpotifyItem(), IExternalTrack {
    abstract val discNumber: Int
    abstract val durationMs: Int
    abstract val href: String?
    abstract override val id: String
    abstract val name: String
    abstract val trackNumber: Int
    abstract val uri: String?
    abstract val artists: List<AT>

    val artist: String
        get() = artists.artistString()

    val duration: Duration
        get() = durationMs.milliseconds

    override val title: String
        get() = name

    fun getTrackDistance(track: Track, artists: List<Artist>): Int {
        val levenshtein = LevenshteinDistance()
        var distance = levenshtein.apply(track.title, name)

        distance += this.artists.getDistances(artists).minOrNull() ?: 0

        // Add number of seconds diffing:
        track.duration?.also { distance += duration.minus(it).inWholeSeconds.toInt().absoluteValue }

        return distance
    }

    open suspend fun toTrackCombo(
        getArtist: suspend (BaseArtist) -> Artist,
        album: Album? = null,
        isLocal: Boolean = false,
        isInLibrary: Boolean = true,
    ): TrackCombo {
        val track = toTrack(albumId = album?.albumId, isInLibrary = isInLibrary)

        return TrackCombo(
            track = track,
            album = album,
            artists = artists.mapIndexed { index, artist ->
                val baseArtist = BaseArtist(name = artist.name, spotifyId = artist.id)
                TrackArtistCredit(artist = getArtist(baseArtist), trackId = track.trackId)
                    .copy(spotifyId = artist.id, position = index)
            },
        )
    }

    open fun toTrack(albumId: UUID?, isInLibrary: Boolean = true) = Track(
        albumId = albumId,
        albumPosition = trackNumber,
        discNumber = discNumber,
        spotifyId = id,
        title = name,
        durationMs = durationMs.toLong(),
        isInLibrary = isInLibrary,
    )
}

data class SpotifySimplifiedTrack(
    override val artists: List<SpotifySimplifiedArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val href: String?,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
    override val uri: String?,
) : AbstractSpotifyTrack<SpotifySimplifiedArtist>()

data class SpotifyTrack(
    override val artists: List<SpotifyArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val href: String?,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
    override val uri: String?,
    val album: SpotifySimplifiedAlbum,
    val popularity: Int,
) : AbstractSpotifyTrack<SpotifyArtist>() {
    data class TrackMatch(
        val distance: Int,
        val spotifyTrack: SpotifyTrack,
    )

    fun matchTrack(track: Track, album: Album? = null, artists: Collection<Artist> = emptyList()) =
        TrackMatch(distance = getTrackDistance(track, album, artists), spotifyTrack = this)

    private fun getTrackDistance(track: Track, album: Album?, artists: Collection<Artist>): Int {
        val levenshtein = LevenshteinDistance()
        val artistDistances = this.artists.getDistances(artists).plus(this.album.artists.getDistances(artists))
        var distance = levenshtein.apply(track.title, name)

        distance += artistDistances.minOrNull() ?: 0

        // Add number of seconds diffing:
        track.duration?.also { distance += duration.minus(it).inWholeSeconds.toInt().absoluteValue }
        // And album diff:
        album?.also { distance += levenshtein.apply(it.title, album.title) }

        return distance
    }

    override fun toTrack(albumId: UUID?, isInLibrary: Boolean): Track =
        super.toTrack(albumId = albumId, isInLibrary = isInLibrary).copy(image = album.images.toMediaStoreImage())

    override suspend fun toTrackCombo(
        getArtist: suspend (BaseArtist) -> Artist,
        album: Album?,
        isLocal: Boolean,
        isInLibrary: Boolean,
    ) = super.toTrackCombo(getArtist = getArtist, album = album, isLocal = isLocal, isInLibrary = isInLibrary)
        .copy(album = album ?: this.album.toAlbum(isLocal = isLocal, isInLibrary = isInLibrary))
}
