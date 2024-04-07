package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.ColumnInfo
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PlaylistPojo(
    @ColumnInfo("Playlist_playlistId") val playlistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Playlist_name") val name: String = "",
    @ColumnInfo("Playlist_created") val created: String = Instant.now().toString(),
    @ColumnInfo("Playlist_updated") val updated: String = Instant.now().toString(),
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
) {
    val totalDuration: Duration
        get() = totalDurationMs.milliseconds

    override fun toString() = name
}
