package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    primaryKeys = ["AlbumStyle_albumId", "AlbumStyle_styleName"],
    indices = [Index("AlbumStyle_styleName")],
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["AlbumStyle_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Style::class,
            parentColumns = ["Style_styleName"],
            childColumns = ["AlbumStyle_styleName"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        )
    ],
)
data class AlbumStyle(
    @ColumnInfo("AlbumStyle_albumId") val albumId: UUID,
    @ColumnInfo("AlbumStyle_styleName") val styleName: String,
)