package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import java.time.Instant
import java.util.UUID


@Entity
data class Playlist(
    @ColumnInfo("Playlist_playlistId") @PrimaryKey override val playlistId: UUID = UUID.randomUUID(),
    @ColumnInfo("Playlist_name") override val name: String,
    @ColumnInfo("Playlist_created") override val created: Instant? = null,
    @ColumnInfo("Playlist_updated") override val updated: Instant? = null,
) : AbstractPlaylist() {
    override fun toString() = name
}
