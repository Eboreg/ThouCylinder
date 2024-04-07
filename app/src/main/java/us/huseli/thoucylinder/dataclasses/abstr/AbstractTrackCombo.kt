package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import android.graphics.Bitmap
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit
import us.huseli.thoucylinder.getCachedFullBitmap

abstract class AbstractTrackCombo : Comparable<AbstractTrackCombo> {
    abstract val track: Track
    abstract val album: Album?
    abstract val artists: List<TrackArtistCredit>
    abstract val albumArtists: List<AlbumArtistCredit>

    val year: Int?
        get() = track.year ?: album?.year

    suspend fun getFullBitmap(context: Context): Bitmap? =
        track.image?.fullUri?.getCachedFullBitmap(context) ?: album?.albumArt?.fullUri?.getCachedFullBitmap(context)

    fun getViewState() = Track.ViewState(
        track = track,
        trackArtists = artists.toImmutableList(),
        album = album,
        albumArtists = albumArtists.toImmutableList(),
        albumArt = album?.albumArt,
    )

    fun toString(
        showAlbumPosition: Boolean = true,
        showArtist: Boolean = true,
        showYear: Boolean = false,
        showArtistIfSameAsAlbumArtist: Boolean = false,
        albumCombo: AlbumWithTracksCombo? = null,
    ): String {
        var string = ""

        if (showAlbumPosition) {
            if (albumCombo != null) string += getPositionString(albumCombo.discCount) + ". "
            else if (track.albumPosition != null) string += "${track.albumPosition}. "
        }
        if (showArtist) {
            val trackArtist = artists.joined()
            val albumArtist = albumArtists.joined()

            if (trackArtist != null && (showArtistIfSameAsAlbumArtist || trackArtist != albumArtist))
                string += "$trackArtist - "
            else if (albumArtist != null && showArtistIfSameAsAlbumArtist)
                string += "$albumArtist - "
        }
        string += track.title
        if (track.year != null && showYear) string += " (${track.year})"

        return string
    }

    override fun equals(other: Any?) = other is AbstractTrackCombo
        && other.track == track
        && other.artists == artists
        && other.albumArtists == albumArtists

    override fun toString(): String = toString(showAlbumPosition = true, showArtist = true, showYear = false)

    private fun getPositionString(albumDiscCount: Int): String =
        if (albumDiscCount > 1 && track.discNumber != null && track.albumPosition != null)
            "${track.discNumber}.${track.albumPosition}"
        else track.albumPosition?.toString() ?: ""

    override fun compareTo(other: AbstractTrackCombo): Int {
        if (track.discNumberNonNull != other.track.discNumberNonNull)
            return track.discNumberNonNull - other.track.discNumberNonNull
        if (track.albumPositionNonNull != other.track.albumPositionNonNull)
            return track.albumPositionNonNull - other.track.albumPositionNonNull
        return track.title.compareTo(other.track.title)
    }

    override fun hashCode(): Int = 31 * (31 * track.hashCode() + artists.hashCode()) + albumArtists.hashCode()
}


fun Collection<AbstractTrackCombo>.tracks(): List<Track> = map { it.track }
