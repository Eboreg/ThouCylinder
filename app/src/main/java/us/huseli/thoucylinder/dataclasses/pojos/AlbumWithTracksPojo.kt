package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import us.huseli.retaintheme.sumOfOrNull
import us.huseli.thoucylinder.dataclasses.PositionPair
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track

data class AlbumWithTracksPojo(
    @Embedded override val album: Album,
    @Ignore override val genres: List<Genre> = emptyList(),
    @Ignore override val styles: List<Style> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "SpotifyAlbum_albumId")
    override val spotifyAlbum: SpotifyAlbum? = null,
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    val tracks: List<Track> = emptyList(),
) : AbstractAlbumPojo() {
    constructor(album: Album, spotifyAlbum: SpotifyAlbum?, tracks: List<Track>) : this(
        album = album,
        genres = emptyList(),
        styles = emptyList(),
        spotifyAlbum = spotifyAlbum,
        tracks = tracks,
    )

    val discCount: Int
        get() = tracks.mapNotNull { it.discNumber }.maxOrNull() ?: 1

    val trackPojos: List<TrackPojo>
        get() = tracks.map { TrackPojo(track = it, album = album) }

    override val trackCount: Int
        get() = tracks.size

    override val durationMs: Long?
        get() = tracks.sumOfOrNull {
            it.metadata?.durationMs ?: it.youtubeVideo?.durationMs ?: it.youtubeVideo?.metadata?.durationMs
        }

    override val minYear: Int?
        get() = tracks.mapNotNull { it.year }.minOrNull()

    override val maxYear: Int?
        get() = tracks.mapNotNull { it.year }.maxOrNull()

    fun getLevenshteinDistance(other: AlbumWithTracksPojo): Double {
        /** N.B. May be misleading if `other` has a different amount of tracks. */
        return album.getLevenshteinDistance(other.album).toDouble().div(tracks.size) +
            tracks.zip(other.tracks).map { (t1, t2) -> t1.getLevenshteinDistance(t2, album.artist) }.average()
    }

    fun getPositionPairs(): List<PositionPair> {
        val pairs = mutableListOf<PositionPair>()
        var discNumber = 1
        var albumPosition = 1

        tracks.forEach { track ->
            if (track.discNumberNonNull != discNumber) {
                discNumber = track.discNumberNonNull
                albumPosition = 1
            }
            if (track.albumPosition != null) albumPosition = track.albumPosition
            pairs.add(PositionPair(discNumber, albumPosition))
            albumPosition++
        }

        return pairs
    }

    fun indexOfTrack(track: Track): Int = tracks.map { it.trackId }.indexOf(track.trackId)

    override fun toString() = album.toString()
}
