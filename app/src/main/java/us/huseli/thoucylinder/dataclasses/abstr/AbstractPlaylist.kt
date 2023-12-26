package us.huseli.thoucylinder.dataclasses.abstr

import us.huseli.thoucylinder.dataclasses.entities.Playlist
import java.time.Instant
import java.util.UUID

abstract class AbstractPlaylist {
    abstract val playlistId: UUID
    abstract val name: String
    abstract val created: Instant?
    abstract val updated: Instant?

    fun toPlaylist() = Playlist(playlistId = playlistId, name = name, created = created, updated = updated)
}
