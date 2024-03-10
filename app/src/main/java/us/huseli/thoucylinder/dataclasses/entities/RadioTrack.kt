package us.huseli.thoucylinder.dataclasses.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Radio::class,
            parentColumns = ["Radio_id"],
            childColumns = ["RadioTrack_radioId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["Track_trackId"],
            childColumns = ["RadioTrack_trackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    primaryKeys = ["RadioTrack_radioId", "RadioTrack_trackId"],
    indices = [Index("RadioTrack_trackId")],
)
data class RadioTrack(
    @ColumnInfo("RadioTrack_radioId") val radioId: UUID,
    @ColumnInfo("RadioTrack_trackId") val trackId: UUID,
)
