package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.plus
import us.huseli.thoucylinder.dataclasses.pojos.reindexed

class TrackQueue(items: List<QueueTrackPojo> = emptyList()) {
    val items = items.reindexed()

    inline fun find(predicate: (QueueTrackPojo) -> Boolean): QueueTrackPojo? = items.find(predicate)

    fun indexOf(pojo: QueueTrackPojo) = items.map { it.queueTrackId }.indexOf(pojo.queueTrackId)

    fun getIndices(queueTracks: List<AbstractQueueTrack>): List<Int> {
        val ids = queueTracks.map { it.queueTrackId }
        return items.mapIndexedNotNull { index, pojo -> if (ids.contains(pojo.queueTrackId)) index else null }
    }

    fun plus(pojo: QueueTrackPojo, index: Int? = null): TrackQueue =
        TrackQueue(items.plus(pojo, index ?: items.size).reindexed())

    fun removeAt(indices: List<Int>): TrackQueue =
        TrackQueue(items.filterIndexed { index, _ -> !indices.contains(index) }.reindexed())
}
