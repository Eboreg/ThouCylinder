package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Genre(
    @PrimaryKey val genreName: String,
)

@Entity(
    primaryKeys = ["albumId", "genreName"],
    indices = [Index("genreName")],
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
            parentColumns = ["genreName"],
            childColumns = ["genreName"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        )
    ],
)
data class AlbumGenre(
    val albumId: UUID,
    val genreName: String,
)
