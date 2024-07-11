package us.huseli.thoucylinder.dataclasses.tag

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import us.huseli.thoucylinder.dataclasses.album.Album

@Entity(
    primaryKeys = ["AlbumTag_albumId", "AlbumTag_tagName"],
    indices = [Index("AlbumTag_tagName")],
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["Album_albumId"],
            childColumns = ["AlbumTag_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["Tag_name"],
            childColumns = ["AlbumTag_tagName"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
)
@Immutable
data class AlbumTag(
    @ColumnInfo("AlbumTag_albumId") val albumId: String,
    @ColumnInfo("AlbumTag_tagName") val tagName: String,
)
