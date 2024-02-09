package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.ColumnInfo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PlaylistPojo(
    @ColumnInfo("Playlist_playlistId") override val playlistId: UUID = UUID.randomUUID(),
    @ColumnInfo("Playlist_name") override val name: String = "",
    @ColumnInfo("Playlist_created") override val created: Instant? = Instant.now(),
    @ColumnInfo("Playlist_updated") override val updated: Instant? = Instant.now(),
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
) : AbstractPlaylist() {
    val totalDuration: Duration
        get() = totalDurationMs.milliseconds

    override fun toString() = name
}
