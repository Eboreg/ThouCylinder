package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
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
    override fun toString() = album.toString()
}