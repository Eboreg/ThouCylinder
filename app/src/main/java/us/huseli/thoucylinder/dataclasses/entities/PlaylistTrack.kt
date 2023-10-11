package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    primaryKeys = ["PlaylistTrack_playlistId", "PlaylistTrack_trackId"],
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
    indices = [Index("PlaylistTrack_trackId")],
)
data class PlaylistTrack(
    @ColumnInfo("PlaylistTrack_playlistId") val playlistId: UUID,
    @ColumnInfo("PlaylistTrack_trackId") val trackId: UUID,
    @ColumnInfo("PlaylistTrack_position") val position: Int,
)
