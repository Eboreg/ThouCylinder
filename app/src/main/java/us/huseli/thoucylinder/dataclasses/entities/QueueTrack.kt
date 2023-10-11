package us.huseli.thoucylinder.dataclasses.entities

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import java.util.UUID


@Entity
data class QueueTrack(
    @ColumnInfo("QueueTrack_queueTrackId") @PrimaryKey override val queueTrackId: UUID = UUID.randomUUID(),
    @ColumnInfo("QueueTrack_trackId") override val trackId: UUID,
    @ColumnInfo("QueueTrack_uri") override val uri: Uri,
    @ColumnInfo("QueueTrack_position") override val position: Int,
) : AbstractQueueTrack()
