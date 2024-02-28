package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.makeFolder
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.retaintheme.extensions.sanitizeFilename
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.toAlbumTags
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractAlbumCombo {
    abstract val album: Album
    abstract val tags: List<Tag>
    abstract val artists: List<AlbumArtistCredit>
    abstract val trackCount: Int
    abstract val durationMs: Long?
    abstract val minYear: Int?
    abstract val maxYear: Int?
    abstract val isPartiallyDownloaded: Boolean

    val albumTags: List<AlbumTag>
        get() = tags.toAlbumTags(album.albumId)

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

    @WorkerThread
    fun createDirectory(downloadRoot: DocumentFile, context: Context): DocumentFile? =
        downloadRoot.makeFolder(context, getSubDirs(context).joinToString("/"), CreateMode.REUSE)

    @WorkerThread
    fun getDirectory(downloadRoot: DocumentFile, context: Context): DocumentFile? {
        var ret: DocumentFile? = downloadRoot
        getSubDirs(context).forEach { dirname ->
            ret = ret?.findFile(dirname)?.takeIf { it.isDirectory }
        }
        return ret
    }

    fun getLevenshteinDistance(other: AbstractAlbumCombo): Int {
        val levenshtein = LevenshteinDistance()
        val distances = mutableListOf<Int>()
        val artistString = artists.joined()
        val otherArtistString = other.artists.joined()

        distances.add(levenshtein.apply(album.title.lowercase(), other.album.title.lowercase()))
        if (artistString != null) {
            distances.add(
                levenshtein.apply(
                    "$artistString - ${album.title}".lowercase(),
                    other.album.title.lowercase(),
                )
            )
            if (otherArtistString != null) distances.add(
                levenshtein.apply(
                    "$artistString - ${album.title}".lowercase(),
                    "$otherArtistString - ${other.album.title}".lowercase(),
                )
            )
        }
        if (otherArtistString != null)
            distances.add(
                levenshtein.apply(
                    album.title.lowercase(),
                    "$otherArtistString - ${other.album.title}".lowercase(),
                )
            )

        return distances.min()
    }

    private fun getSubDirs(context: Context): List<String> = listOf(
        artists.joined()?.sanitizeFilename() ?: context.getString(R.string.unknown_artist),
        album.title.sanitizeFilename(),
    )
}
