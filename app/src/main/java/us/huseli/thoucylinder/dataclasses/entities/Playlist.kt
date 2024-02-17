package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID


@Entity
data class Playlist(
    @ColumnInfo("Playlist_playlistId") @PrimaryKey val playlistId: UUID = UUID.randomUUID(),
    @ColumnInfo("Playlist_name") val name: String,
    @ColumnInfo("Playlist_created") val created: Instant? = Instant.now(),
    @ColumnInfo("Playlist_updated") val updated: Instant? = Instant.now(),
) {
    override fun toString() = name
}
