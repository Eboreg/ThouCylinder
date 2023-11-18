package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["SpotifyTrackArtist_trackId", "SpotifyTrackArtist_artistId"],
    indices = [Index("SpotifyTrackArtist_artistId")],
    foreignKeys = [
        ForeignKey(
            entity = SpotifyTrack::class,
            parentColumns = ["SpotifyTrack_id"],
            childColumns = ["SpotifyTrackArtist_trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SpotifyArtist::class,
            parentColumns = ["SpotifyArtist_id"],
            childColumns = ["SpotifyTrackArtist_artistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class SpotifyTrackArtist(
    @ColumnInfo("SpotifyTrackArtist_trackId") val trackId: String,
    @ColumnInfo("SpotifyTrackArtist_artistId") val artistId: String,
)