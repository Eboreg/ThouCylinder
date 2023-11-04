package us.huseli.thoucylinder.dataclasses.pojos

import androidx.room.Embedded
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

open class TrackPojo(
    @Embedded open val track: Track,
    @Embedded open val album: Album?,
) {
    val trackId: UUID
        get() = track.trackId

    val artist: String?
        get() = track.artist ?: album?.artist

    fun copy(track: Track = this.track, album: Album? = this.album) = TrackPojo(track = track, album = album)

    fun toQueueTrackPojo(index: Int): QueueTrackPojo? =
        track.playUri?.let { uri -> QueueTrackPojo(track = track, uri = uri, position = index, album = album) }

    override fun equals(other: Any?) = other is TrackPojo && other.track == track && other.album == album

    override fun hashCode(): Int = 31 * track.hashCode() + (album?.hashCode() ?: 0)
}

fun List<TrackPojo>.toQueueTrackPojos(startIndex: Int = 0): List<QueueTrackPojo> {
    var offset = 0
    return mapNotNull { pojo -> pojo.toQueueTrackPojo(startIndex + offset)?.also { offset++ } }
}
