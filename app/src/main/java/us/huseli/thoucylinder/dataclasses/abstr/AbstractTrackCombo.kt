package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackArtistCredit

abstract class AbstractTrackCombo : Comparable<AbstractTrackCombo> {
    abstract val track: Track
    abstract val album: Album?
    abstract val artists: List<TrackArtistCredit>
    abstract val albumArtist: String?

    val year: Int?
        get() = track.year ?: album?.year

    suspend fun getFullBitmap(context: Context) =
        track.image?.getFullBitmap(context) ?: album?.albumArt?.getFullBitmap(context)

    suspend fun getFullImageBitmap(context: Context): ImageBitmap? =
        track.image?.getFullImageBitmap(context) ?: album?.albumArt?.getFullImageBitmap(context)

    fun toQueueTrackCombo(): QueueTrackCombo? = track.playUri?.let { uri ->
        QueueTrackCombo(
            track = track,
            uri = uri,
            album = album,
            albumArtist = albumArtist,
            artists = artists,
        )
    }

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
            val artistString = artists.joined()

            if (artistString != null && (showArtistIfSameAsAlbumArtist || artistString != albumArtist))
                string += "$artistString - "
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
        && other.albumArtist == albumArtist

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

    override fun hashCode(): Int = 31 * (31 * track.hashCode() + artists.hashCode()) + (albumArtist?.hashCode() ?: 0)
}


fun Collection<AbstractTrackCombo>.tracks(): List<Track> = map { it.track }
