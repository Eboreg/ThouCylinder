package us.huseli.thoucylinder.dataclasses.abstr

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import java.util.UUID

abstract class AbstractTrackPojo {
    abstract val track: Track
    abstract val album: Album?

    val trackId: UUID
        get() = track.trackId

    val artist: String?
        get() = track.artist ?: album?.artist

    suspend fun getFullImage(context: Context): ImageBitmap? =
        track.getFullImage(context)?.asImageBitmap() ?: album?.getFullImage(context)?.asImageBitmap()

    suspend fun getThumbnail(context: Context): ImageBitmap? =
        track.getThumbnail(context)?.asImageBitmap() ?: album?.getThumbnail(context)?.asImageBitmap()

    fun toQueueTrackPojo(index: Int): QueueTrackPojo? =
        track.playUri?.let { uri -> QueueTrackPojo(track = track, uri = uri, position = index, album = album) }

    override fun equals(other: Any?) = other is AbstractTrackPojo && other.track == track && other.album == album

    override fun hashCode(): Int = 31 * track.hashCode() + (album?.hashCode() ?: 0)
}

fun List<AbstractTrackPojo>.toQueueTrackPojos(startIndex: Int = 0): List<QueueTrackPojo> {
    var offset = 0
    return mapNotNull { pojo -> pojo.toQueueTrackPojo(startIndex + offset)?.also { offset++ } }
}
