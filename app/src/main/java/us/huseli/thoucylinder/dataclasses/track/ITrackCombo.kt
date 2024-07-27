package us.huseli.thoucylinder.dataclasses.track

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.ITrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.TrackArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined

interface ITrackCombo : Comparable<ITrackCombo> {
    val track: Track
    val album: IAlbum?
    val trackArtists: List<ITrackArtistCredit>
    val albumArtists: List<IAlbumArtistCredit>

    val artists: List<IArtistCredit>
        get() = trackArtists.takeIf { it.isNotEmpty() } ?: albumArtists

    val artistString: String?
        get() = trackArtists.joined() ?: albumArtists.joined()

    val fullImageUrl: String?
        get() = album?.fullImageUrl ?: track.fullImageUrl

    val thumbnailUrl: String?
        get() = album?.thumbnailUrl ?: track.thumbnailUrl

    val year: Int?
        get() = track.year ?: album?.year

    override fun compareTo(other: ITrackCombo): Int {
        if (track.discNumberNonNull != other.track.discNumberNonNull)
            return track.discNumberNonNull - other.track.discNumberNonNull
        if (track.albumPositionNonNull != other.track.albumPositionNonNull)
            return track.albumPositionNonNull - other.track.albumPositionNonNull
        return track.title.compareTo(other.track.title)
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
            val trackArtist = trackArtists.joined()
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

    fun toUiState(isSelected: Boolean = false): TrackUiState = track.toUiState(isSelected = isSelected).copy(
        albumTitle = album?.title,
        artists = artists
            .map { AbstractTrackUiState.Artist.fromArtistCredit(it) }
            .toImmutableList(),
        artistString = artistString,
        fullImageUrl = album?.fullImageUrl ?: track.fullImageUrl,
        musicBrainzReleaseGroupId = album?.musicBrainzReleaseGroupId,
        musicBrainzReleaseId = album?.musicBrainzReleaseId,
        thumbnailUrl = album?.thumbnailUrl ?: track.thumbnailUrl,
    )

    fun withTrack(track: Track): ITrackCombo

    private fun getPositionString(albumDiscCount: Int): String =
        if (albumDiscCount > 1 && track.discNumber != null && track.albumPosition != null)
            "${track.discNumber}.${track.albumPosition}"
        else track.albumPosition?.toString() ?: ""
}


fun Iterable<ITrackCombo>.tracks(): List<Track> = map { it.track }

fun Iterable<ITrackCombo>.toUiStates(): ImmutableList<TrackUiState> = map { it.toUiState() }.toImmutableList()


@Suppress("UNCHECKED_CAST")
fun <T : ITrackCombo> Iterable<T>.withTracks(tracks: Iterable<Track>): List<T> =
    zip(tracks).map { (trackCombo, track) -> trackCombo.withTrack(track) as T }


interface ISavedTrackCombo : ITrackCombo {
    override val trackArtists: List<TrackArtistCredit>
    override val albumArtists: List<AlbumArtistCredit>
    override val album: Album?
}
