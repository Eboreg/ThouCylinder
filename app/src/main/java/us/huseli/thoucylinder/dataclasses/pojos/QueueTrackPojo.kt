package us.huseli.thoucylinder.dataclasses.pojos

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.room.ColumnInfo
import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

data class QueueTrackPojo(
    @Embedded val track: Track,
    @Embedded val album: Album?,
    @ColumnInfo("QueueTrack_uri") override val uri: Uri,
    @ColumnInfo("QueueTrack_queueTrackId") override val queueTrackId: UUID = UUID.randomUUID(),
    @ColumnInfo("QueueTrack_position") override val position: Int = 0,
) : AbstractQueueTrack() {
    val queueTrack: QueueTrack
        get() = QueueTrack(queueTrackId = queueTrackId, trackId = track.trackId, uri = uri, position = position)
    val artist: String?
        get() = track.artist ?: album?.artist
    override val trackId: UUID
        get() = track.trackId

    override fun equals(other: Any?) = other is QueueTrackPojo &&
        other.track.trackId == track.trackId &&
        other.queueTrackId == queueTrackId &&
        other.uri == uri

    override fun hashCode(): Int = 31 * (31 * track.trackId.hashCode() + uri.hashCode()) + queueTrackId.hashCode()

    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(queueTrackId.toString())
        .setUri(uri)
        .build()
}


fun List<QueueTrackPojo>.reindexed(): List<QueueTrackPojo> = mapIndexed { index, pojo -> pojo.copy(position = index) }

fun List<QueueTrackPojo>.plus(item: QueueTrackPojo, index: Int): List<QueueTrackPojo> =
    toMutableList().apply { add(index, item) }.toList()
