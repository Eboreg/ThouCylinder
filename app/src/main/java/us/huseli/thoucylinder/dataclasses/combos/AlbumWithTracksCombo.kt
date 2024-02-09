package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.retaintheme.extensions.sumOfOrNull
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.getLevenshteinDistance

data class AlbumWithTracksCombo(
    @Embedded override val album: Album,
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
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    val tracks: List<Track> = emptyList(),
) : AbstractAlbumCombo() {
    val discCount: Int
        get() = tracks.mapNotNull { it.discNumber }.maxOrNull() ?: 1

    val trackCombos: List<TrackCombo>
        get() = tracks.map { TrackCombo(track = it, album = album) }

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

    override val isPartiallyDownloaded: Boolean
        get() = tracks.any { it.isDownloaded } && tracks.any { !it.isDownloaded }

    fun getLevenshteinDistance(other: AlbumWithTracksCombo): Double {
        /** N.B. May be misleading if `other` has a different amount of tracks. */
        return album.getLevenshteinDistance(other.album).toDouble().div(tracks.size) +
            tracks.getLevenshteinDistance(other.tracks, album.artist)
    }

    fun indexOfTrack(track: Track): Int = tracks.map { it.trackId }.indexOf(track.trackId)

    fun sorted(): AlbumWithTracksCombo = copy(
        tracks = tracks.sorted(),
        genres = genres.sortedBy { it.genreName.length },
        styles = styles.sortedBy { it.styleName.length },
    )

    override fun toString() = album.toString()
}
