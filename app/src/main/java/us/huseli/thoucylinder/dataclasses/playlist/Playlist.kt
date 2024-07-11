package us.huseli.thoucylinder.dataclasses.playlist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID


@Entity
@Immutable
data class Playlist(
    @ColumnInfo("Playlist_playlistId") @PrimaryKey val playlistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Playlist_name") val name: String,
    @ColumnInfo("Playlist_created") val created: String = Instant.now().toString(),
    @ColumnInfo("Playlist_updated") val updated: String = Instant.now().toString(),
) {
    override fun toString() = name
}
