package us.huseli.thoucylinder.dataclasses

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Genre(
    @PrimaryKey val genreId: String,
)

@Entity(
    primaryKeys = ["albumId", "genreId"],
    indices = [Index("genreId")],
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["genreId"],
            childColumns = ["genreId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        )
    ],
)
data class AlbumGenre(
    val albumId: UUID,
    val genreId: String,
)
