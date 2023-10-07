package us.huseli.thoucylinder.dataclasses

import androidx.room.Embedded
import androidx.room.Relation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


data class AlbumPojo(
    @Embedded val album: Album,
    val durationMs: Long? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val trackCount: Int? = null,
    @Relation(parentColumn = "albumId", entityColumn = "albumId", entity = AlbumGenre::class)
    val genres: List<Genre> = emptyList(),
    @Relation(parentColumn = "albumId", entityColumn = "albumId", entity = AlbumStyle::class)
    val styles: List<Style> = emptyList(),
) {
    private val years: Pair<Int, Int>?
        get() {
            val year = this.album.year
            val minYear = this.minYear
            val maxYear = this.maxYear

            return if (year != null) Pair(year, year)
            else if (minYear != null && maxYear != null) Pair(minYear, maxYear)
            else null
        }

    val yearString: String?
        get() = years?.let { (min, max) ->
            if (min == max) min.toString()
            else "$min–$max"
        }

    val duration: Duration?
        get() = durationMs?.milliseconds

    override fun toString() = album.toString()
}


data class AlbumWithTracksPojo(
    @Embedded val album: Album,
    @Relation(parentColumn = "albumId", entityColumn = "albumId", entity = AlbumGenre::class)
    val genres: List<Genre> = emptyList(),
    @Relation(parentColumn = "albumId", entityColumn = "albumId", entity = AlbumStyle::class)
    val styles: List<Style> = emptyList(),
    @Relation(parentColumn = "albumId", entityColumn = "albumId")
    val tracks: List<Track> = emptyList(),
) {
    override fun toString() = album.toString()
}
