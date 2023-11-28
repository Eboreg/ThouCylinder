package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    primaryKeys = ["AlbumGenre_albumId", "AlbumGenre_genreName"],
    indices = [Index("AlbumGenre_genreName")],
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["AlbumGenre_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["Genre_genreName"],
            childColumns = ["AlbumGenre_genreName"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
)
data class AlbumGenre(
    @ColumnInfo("AlbumGenre_albumId") val albumId: UUID,
    @ColumnInfo("AlbumGenre_genreName") val genreName: String,
)
