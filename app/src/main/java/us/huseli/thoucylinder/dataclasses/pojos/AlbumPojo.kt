package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style


data class AlbumPojo(
    @Embedded override val album: Album,
    override val durationMs: Long? = null,
    override val minYear: Int? = null,
    override val maxYear: Int? = null,
    override val trackCount: Int,
    override val isPartiallyDownloaded: Boolean,
    @Relation(
        entity = Genre::class,
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
        entity = Style::class,
        parentColumn = "Album_albumId",
        entityColumn = "Style_styleName",
        associateBy = Junction(
            value = AlbumStyle::class,
            parentColumn = "AlbumStyle_albumId",
            entityColumn = "AlbumStyle_styleName",
        )
    )
    override val styles: List<Style> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "SpotifyAlbum_albumId")
    override val spotifyAlbum: SpotifyAlbum? = null,
    @Relation(parentColumn = "Album_albumId", entityColumn = "LastFmAlbum_albumId")
    override val lastFmAlbum: LastFmAlbum? = null,
) : AbstractAlbumPojo() {
    fun sorted(): AlbumPojo = copy(
        genres = genres.sortedBy { it.genreName.length },
        styles = styles.sortedBy { it.styleName.length },
    )

    fun sortGenres() = copy(genres = genres.sortedBy { it.genreName.length })

    fun sortStyles() = copy(styles = styles.sortedBy { it.styleName.length })

    override fun toString() = album.toString()
}

fun Collection<AlbumPojo>.sortGenres() = map { it.sortGenres() }

fun Collection<AlbumPojo>.sortStyles() = map { it.sortStyles() }
