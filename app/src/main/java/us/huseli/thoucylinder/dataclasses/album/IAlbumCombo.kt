package us.huseli.thoucylinder.dataclasses.album

import kotlinx.collections.immutable.toImmutableList
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.thoucylinder.dataclasses.artist.AlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.IAlbumArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.joined
import us.huseli.thoucylinder.interfaces.IStringIdItem

interface IAlbumCombo<out A : IAlbum> : IStringIdItem {
    val album: A
    val artists: List<IAlbumArtistCredit>
    val minYear: Int?
    val maxYear: Int?
    val isPartiallyDownloaded: Boolean
    val unplayableTrackCount: Int
    val isSaved: Boolean
    val isDownloadable: Boolean

    val artistNames: List<String>
        get() = artists.map { it.name }

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

    override val id: String
        get() = album.albumId

    fun getLevenshteinDistance(albumTitle: String, artistString: String?): Int {
        val levenshtein = LevenshteinDistance()
        val distances = mutableListOf<Int>()
        val ourArtistString = artists.joined()

        distances.add(levenshtein.apply(album.title.lowercase(), albumTitle.lowercase()))
        if (ourArtistString != null) {
            distances.add(
                levenshtein.apply(
                    "$ourArtistString - ${album.title}".lowercase(),
                    albumTitle.lowercase(),
                )
            )
            if (artistString != null) distances.add(
                levenshtein.apply(
                    "$ourArtistString - ${album.title}".lowercase(),
                    "$artistString - $albumTitle".lowercase(),
                )
            )
        }
        if (artistString != null)
            distances.add(
                levenshtein.apply(
                    album.title.lowercase(),
                    "$artistString - $albumTitle".lowercase(),
                )
            )

        return distances.min()
    }

    fun toImportableUiState(playCount: Int? = null) =
        album.toImportableUiState(playCount = playCount).copy(
            artistString = artists.joined(),
            artists = artists.toImmutableList(),
            isDownloadable = isDownloadable,
        )

    fun toUiState(isSelected: Boolean = false) = album.toUiState().copy(
        artistString = artists.joined(),
        artists = artists.toImmutableList(),
        isDownloadable = isDownloadable,
        isPartiallyDownloaded = isPartiallyDownloaded,
        isSelected = isSelected,
        unplayableTrackCount = unplayableTrackCount,
        yearString = yearString,
    )
}

interface IUnsavedAlbumCombo : IAlbumCombo<IAlbum> {
    override val isSaved: Boolean
        get() = false
}

interface ISavedAlbumCombo : IAlbumCombo<Album> {
    override val album: Album
    override val artists: List<AlbumArtistCredit>
    override val isSaved: Boolean
        get() = true
}

fun Iterable<IAlbumCombo<*>>.toUiStates() = map { it.toUiState() }.toImmutableList()
