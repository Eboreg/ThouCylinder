package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmArtist
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["Track_trackId"],
            childColumns = ["LastFmTrack_trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LastFmAlbum::class,
            parentColumns = ["LastFmAlbum_musicBrainzId"],
            childColumns = ["LastFmTrack_lastFmAlbumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("LastFmTrack_lastFmAlbumId")],
)
data class LastFmTrack(
    @ColumnInfo("LastFmTrack_trackId") @PrimaryKey val trackId: UUID,
    @ColumnInfo("LastFmTrack_duration") val duration: Int,
    @ColumnInfo("LastFmTrack_url") val url: String,
    @ColumnInfo("LastFmTrack_name") val name: String,
    @Embedded("LastFmTrack_artist_") val artist: LastFmArtist,
    @ColumnInfo("LastFmTrack_lastFmAlbumId") val albumId: String,
    @ColumnInfo("LastFmTrack_musicBrainzId") val musicBrainzId: String?,
)
