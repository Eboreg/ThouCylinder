package us.huseli.thoucylinder.dataclasses

import androidx.room.Entity
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
)
data class AlbumGenre(
    val albumId: UUID,
    val genreId: String,
)
