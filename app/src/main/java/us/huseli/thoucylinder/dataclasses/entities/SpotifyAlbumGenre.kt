package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["SpotifyAlbumGenre_albumId", "SpotifyAlbumGenre_genreName"],
    indices = [Index("SpotifyAlbumGenre_genreName")],
    foreignKeys = [
        ForeignKey(
            entity = SpotifyAlbum::class,
            parentColumns = ["SpotifyAlbum_id"],
            childColumns = ["SpotifyAlbumGenre_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["Genre_genreName"],
            childColumns = ["SpotifyAlbumGenre_genreName"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        )
    ],
)
data class SpotifyAlbumGenre(
    @ColumnInfo("SpotifyAlbumGenre_albumId") val albumId: String,
    @ColumnInfo("SpotifyAlbumGenre_genreName") val genreName: String,
)