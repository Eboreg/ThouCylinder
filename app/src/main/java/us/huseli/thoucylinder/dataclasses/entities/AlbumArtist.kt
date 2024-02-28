package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import us.huseli.retaintheme.extensions.combineEquals
import java.util.UUID

@Entity(
    primaryKeys = ["AlbumArtist_albumId", "AlbumArtist_artistId"],
    indices = [Index("AlbumArtist_artistId")],
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["AlbumArtist_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Artist::class,
            parentColumns = ["Artist_id"],
            childColumns = ["AlbumArtist_artistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class AlbumArtist(
    @ColumnInfo("AlbumArtist_albumId") val albumId: UUID,
    @ColumnInfo("AlbumArtist_artistId") val artistId: UUID,
    @ColumnInfo("AlbumArtist_joinPhrase") val joinPhrase: String = "/",
    @ColumnInfo("AlbumArtist_position") val position: Int = 0,
) : Comparable<AlbumArtist> {
    override fun compareTo(other: AlbumArtist): Int = position - other.position
}

fun Iterable<AlbumArtist>.enumerate() = combineEquals { a, b -> a.albumId == b.albumId }
    .flatMap { albumArtists -> albumArtists.mapIndexed { index, albumArtist -> albumArtist.copy(position = index) } }
