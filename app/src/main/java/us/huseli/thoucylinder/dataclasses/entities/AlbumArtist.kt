package us.huseli.thoucylinder.dataclasses.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
@Immutable
data class AlbumArtist(
    @ColumnInfo("AlbumArtist_albumId") val albumId: String,
    @ColumnInfo("AlbumArtist_artistId") val artistId: String,
    @ColumnInfo("AlbumArtist_joinPhrase") val joinPhrase: String = "/",
    @ColumnInfo("AlbumArtist_position") val position: Int = 0,
) : Comparable<AlbumArtist> {
    override fun compareTo(other: AlbumArtist): Int = position - other.position
}
