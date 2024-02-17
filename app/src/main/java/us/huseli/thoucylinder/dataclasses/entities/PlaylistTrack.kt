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
            entity = Playlist::class,
            parentColumns = ["Playlist_playlistId"],
            childColumns = ["PlaylistTrack_playlistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["Track_trackId"],
            childColumns = ["PlaylistTrack_trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("PlaylistTrack_trackId"), Index("PlaylistTrack_position"), Index("PlaylistTrack_playlistId")],
)
data class PlaylistTrack(
    @PrimaryKey @ColumnInfo("PlaylistTrack_id") val id: UUID = UUID.randomUUID(),
    @ColumnInfo("PlaylistTrack_playlistId") val playlistId: UUID,
    @ColumnInfo("PlaylistTrack_trackId") val trackId: UUID,
    @ColumnInfo("PlaylistTrack_position") val position: Int,
)
