package us.huseli.thoucylinder.dataclasses

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Style(
    @PrimaryKey val styleId: String,
)

@Entity(
    primaryKeys = ["albumId", "styleId"],
    indices = [Index("styleId")],
)
data class AlbumStyle(
    val albumId: UUID,
    val styleId: String,
)
