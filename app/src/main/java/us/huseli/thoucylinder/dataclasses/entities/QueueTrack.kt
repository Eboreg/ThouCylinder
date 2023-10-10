package us.huseli.thoucylinder.dataclasses.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

abstract class AbstractQueueTrack {
    abstract val queueTrackId: UUID
    abstract val trackId: UUID
    abstract val uri: Uri
    abstract val position: Int
}


@Entity
data class QueueTrack(
    @PrimaryKey override val queueTrackId: UUID = UUID.randomUUID(),
    override val trackId: UUID,
    override val uri: Uri,
    override val position: Int,
) : AbstractQueueTrack()
