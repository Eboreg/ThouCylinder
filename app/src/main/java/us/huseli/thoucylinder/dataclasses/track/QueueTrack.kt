package us.huseli.thoucylinder.dataclasses.track

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID


@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["Track_trackId"],
            childColumns = ["QueueTrack_trackId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("QueueTrack_trackId")],
)
@Immutable
data class QueueTrack(
    @ColumnInfo("QueueTrack_queueTrackId") @PrimaryKey val queueTrackId: String = UUID.randomUUID().toString(),
    @ColumnInfo("QueueTrack_trackId") val trackId: String,
    @ColumnInfo("QueueTrack_position") val position: Int,
)
