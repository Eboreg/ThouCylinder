package us.huseli.thoucylinder.dataclasses.abstr

import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractAlbumCombo {
    abstract val album: Album
    abstract val tags: List<Tag>
    abstract val trackCount: Int
    abstract val durationMs: Long?
    abstract val minYear: Int?
    abstract val maxYear: Int?
    abstract val isPartiallyDownloaded: Boolean

    val albumTags: List<AlbumTag>
        get() = tags.map { AlbumTag(albumId = album.albumId, tagName = it.name) }

    val duration: Duration?
        get() = durationMs?.milliseconds

    private val years: Pair<Int, Int>?
        get() {
            val year = this.album.year?.takeIf { it > 1000 }
            val minYear = this.minYear?.takeIf { it > 1000 }
            val maxYear = this.maxYear?.takeIf { it > 1000 }

            return if (year != null) Pair(year, year)
            else if (minYear != null && maxYear != null) Pair(minYear, maxYear)
            else null
        }

    val yearString: String?
        get() = years?.let { (min, max) ->
            if (min == max) min.toString()
            else "$min–$max"
        }
}
