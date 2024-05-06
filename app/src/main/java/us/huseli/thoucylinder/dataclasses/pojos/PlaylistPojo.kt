package us.huseli.thoucylinder.dataclasses.pojos

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class PlaylistPojo(
    @ColumnInfo("Playlist_playlistId") val playlistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Playlist_name") val name: String = "",
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
) {
    val totalDuration: Duration
        get() = totalDurationMs.milliseconds

    override fun toString() = name
}
