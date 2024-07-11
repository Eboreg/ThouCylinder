package us.huseli.thoucylinder.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.UnsavedTrackCombo
import us.huseli.thoucylinder.interfaces.IExternalTrack
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

    override fun toTrackCombo(isInLibrary: Boolean, album: IAlbum?): UnsavedTrackCombo {
        val track = toTrack(isInLibrary = isInLibrary, albumId = album?.albumId)

        return UnsavedTrackCombo(
            track = track,
            trackArtists = artists.toNativeTrackArtists(trackId = track.trackId),
            album = album,
        )
    }

    open fun toTrack(isInLibrary: Boolean, albumId: String? = null) = Track(
        albumPosition = trackNumber,
        discNumber = discNumber,
        spotifyId = id,
        title = name,
        durationMs = durationMs.toLong(),
        isInLibrary = isInLibrary,
        albumId = albumId,
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
    // "A link to the Web API endpoint providing full details of the track":
    override val href: String?,
    override val id: String,
    override val name: String,
    @SerializedName("preview_url")
    val previewUrl: String?,
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

    fun matchTrack(track: Track, album: Album? = null, artistNames: Collection<String> = emptyList()) =
        TrackMatch(distance = getTrackDistance(track, album, artistNames), spotifyTrack = this)

    private fun getTrackDistance(track: Track, album: Album?, artistNames: Collection<String>): Int {
        val levenshtein = LevenshteinDistance()
        val artistDistances = this.artists.getDistances(artistNames).plus(this.album.artists.getDistances(artistNames))
        var distance = levenshtein.apply(track.title, name)

        distance += artistDistances.minOrNull() ?: 0

        // Add number of seconds diffing:
        track.duration?.also { distance += duration.minus(it).inWholeSeconds.toInt().absoluteValue }
        // And album diff:
        album?.also { distance += levenshtein.apply(it.title, album.title) }

        return distance
    }

    override fun toTrack(isInLibrary: Boolean, albumId: String?): Track =
        super.toTrack(isInLibrary = isInLibrary, albumId = albumId).copy(image = album.images.toMediaStoreImage())

    override fun toTrackCombo(isInLibrary: Boolean, album: IAlbum?): UnsavedTrackCombo {
        val finalAlbum = album ?: this.album.toAlbum(isLocal = false, isInLibrary = isInLibrary)
        val trackCombo = super.toTrackCombo(isInLibrary, album = finalAlbum)

        return trackCombo.copy(
            albumArtists = this.album.artists.toNativeAlbumArtists(albumId = finalAlbum.albumId),
        )
    }
}


data class SpotifyTrackIdPair(val spotifyTrackId: String, val trackId: String)
