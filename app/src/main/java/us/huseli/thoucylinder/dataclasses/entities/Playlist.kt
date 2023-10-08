package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

abstract class AbstractPlaylist {
    abstract val playlistId: UUID
    abstract val name: String
    abstract val created: Instant?
    abstract val updated: Instant?
}


@Entity
data class Playlist(
    @PrimaryKey override val playlistId: UUID = UUID.randomUUID(),
    override val name: String,
    override val created: Instant? = null,
    override val updated: Instant? = null,
) : AbstractPlaylist() {
    override fun toString() = name
}


@Entity(
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["trackId"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackId")],
)
data class PlaylistTrack(
    val playlistId: UUID,
    val trackId: UUID,
    val position: Int,
)
