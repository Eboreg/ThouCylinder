package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.dataclasses.entities.AbstractPlaylist
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PlaylistPojo(
    override val playlistId: UUID = UUID.randomUUID(),
    override val name: String = "",
    override val created: Instant? = null,
    override val updated: Instant? = null,
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
) : AbstractPlaylist() {
    val totalDuration: Duration
        get() = totalDurationMs.milliseconds

    override fun toString() = name
}