package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
data class AlbumTag(
    @ColumnInfo("AlbumTag_albumId") val albumId: String,
    @ColumnInfo("AlbumTag_tagName") val tagName: String,
)
