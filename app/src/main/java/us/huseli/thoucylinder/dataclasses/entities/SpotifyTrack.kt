package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = SpotifyAlbum::class,
            parentColumns = ["SpotifyAlbum_id"],
            childColumns = ["SpotifyTrack_spotifyAlbumId"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["Track_trackId"],
            childColumns = ["SpotifyTrack_trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("SpotifyTrack_spotifyAlbumId"), Index("SpotifyTrack_trackId")],
)
data class SpotifyTrack(
    @ColumnInfo("SpotifyTrack_discNumber") val discNumber: Int,
    @ColumnInfo("SpotifyTrack_durationMs") val durationMs: Int,
    @ColumnInfo("SpotifyTrack_href") val href: String,
    @ColumnInfo("SpotifyTrack_id") @PrimaryKey val id: String,
    @ColumnInfo("SpotifyTrack_name") val name: String,
    @ColumnInfo("SpotifyTrack_trackNumber") val trackNumber: Int,
    @ColumnInfo("SpotifyTrack_uri") val uri: String,
    @ColumnInfo("SpotifyTrack_spotifyAlbumId") val albumId: String? = null,
    @ColumnInfo("SpotifyTrack_trackId") val trackId: UUID? = null,
)
