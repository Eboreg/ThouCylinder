package us.huseli.thoucylinder.dataclasses.pojos

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.documentfile.provider.DocumentFile
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.retaintheme.sumOfOrNull
import us.huseli.thoucylinder.dataclasses.PositionPair
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.getParentDirectory
import us.huseli.thoucylinder.matchFilesRecursive

data class AlbumWithTracksPojo(
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
    @Relation(parentColumn = "Album_albumId", entityColumn = "LastFmAlbum_albumId")
    override val lastFmAlbum: LastFmAlbum? = null,
) : AbstractAlbumPojo() {
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

    override val isPartiallyDownloaded: Boolean
        get() = tracks.any { it.isDownloaded } && tracks.any { !it.isDownloaded }

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

    @WorkerThread
    fun listCoverImages(context: Context, includeThumbnails: Boolean = false): List<DocumentFile> {
        val filenameRegex =
            if (includeThumbnails) Regex("^cover(-thumbnail)?\\..*", RegexOption.IGNORE_CASE)
            else Regex("^cover\\..*", RegexOption.IGNORE_CASE)

        return listTrackParentDirectories(context)
            .map { it.matchFilesRecursive(filenameRegex, Regex("^image/.*")) }
            .flatten()
            .distinctBy { it.uri.path }
    }

    @WorkerThread
    fun listTrackParentDirectories(context: Context): List<DocumentFile> =
        tracks.mapNotNull { it.getDocumentFile(context)?.getParentDirectory(context) }.distinctBy { it.uri.path }

    fun sorted(): AlbumWithTracksPojo = copy(
        tracks = tracks.sorted(),
        genres = genres.sortedBy { it.genreName.length },
        styles = styles.sortedBy { it.styleName.length },
    )

    override fun toString() = album.toString()
}
