package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.PositionPair
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track

data class AlbumWithTracksPojo(
    @Embedded override val album: Album,
    @Relation(
        parentColumn = "Album_albumId",
        entityColumn = "Genre_genreName",
        associateBy = Junction(
            value = AlbumGenre::class,
            parentColumn = "AlbumGenre_albumId",
            entityColumn = "AlbumGenre_genreName",
        )
    )
    override val genres: List<Genre> = emptyList(),
    @Relation(
        parentColumn = "Album_albumId",
        entityColumn = "Style_styleName",
        associateBy = Junction(
            value = AlbumStyle::class,
            parentColumn = "AlbumStyle_albumId",
            entityColumn = "AlbumStyle_styleName",
        )
    )
    override val styles: List<Style> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    val tracks: List<Track> = emptyList(),
) : AbstractAlbumPojo() {
    val discCount: Int
        get() = tracks.mapNotNull { it.discNumber }.takeIf { it.isNotEmpty() }?.max() ?: 1

    val trackPojos: List<TrackPojo>
        get() = tracks.map { TrackPojo(track = it, album = album) }

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