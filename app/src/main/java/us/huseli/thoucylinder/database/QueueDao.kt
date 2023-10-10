@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.thoucylinder.dataclasses.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack

@Dao
interface QueueDao {
    @Query("DELETE FROM QueueTrack")
    suspend fun _clearQueue()

    @Insert
    suspend fun _insertQueueTracks(vararg queueTracks: QueueTrack)

    @Query("SELECT t.*, qt.uri, qt.queueTrackId, qt.position FROM QueueTrack qt JOIN Track t ON t.trackId = qt.trackId ORDER BY qt.position")
    suspend fun getQueue(): List<QueueTrackPojo>

    @Transaction
    suspend fun setQueue(queue: List<QueueTrackPojo>) {
        _clearQueue()
        _insertQueueTracks(*queue.map { it.queueTrack }.toTypedArray())
    }
}
