package us.huseli.thoucylinder.dataclasses.entities

import android.net.Uri
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
data class QueueTrack(
    @ColumnInfo("QueueTrack_queueTrackId") @PrimaryKey val queueTrackId: UUID = UUID.randomUUID(),
    @ColumnInfo("QueueTrack_trackId") val trackId: UUID,
    @ColumnInfo("QueueTrack_uri") val uri: Uri,
    @ColumnInfo("QueueTrack_position") val position: Int,
)
