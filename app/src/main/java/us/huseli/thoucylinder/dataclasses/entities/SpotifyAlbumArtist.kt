package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["SpotifyAlbumArtist_albumId", "SpotifyAlbumArtist_artistId"],
    indices = [Index("SpotifyAlbumArtist_artistId")],
    foreignKeys = [
        ForeignKey(
            entity = SpotifyAlbum::class,
            parentColumns = ["SpotifyAlbum_id"],
            childColumns = ["SpotifyAlbumArtist_albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SpotifyArtist::class,
            parentColumns = ["SpotifyArtist_id"],
            childColumns = ["SpotifyAlbumArtist_artistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class SpotifyAlbumArtist(
    @ColumnInfo("SpotifyAlbumArtist_albumId") val albumId: String,
    @ColumnInfo("SpotifyAlbumArtist_artistId") val artistId: String,
)