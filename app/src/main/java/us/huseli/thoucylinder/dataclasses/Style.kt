package us.huseli.thoucylinder.dataclasses

import androidx.room.Entity
import androidx.room.ForeignKey
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
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Style::class,
            parentColumns = ["styleId"],
            childColumns = ["styleId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        )
    ],
)
data class AlbumStyle(
    val albumId: UUID,
    val styleId: String,
)
