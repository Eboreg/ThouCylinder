@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo

@Dao
interface QueueDao {
    @Query("DELETE FROM QueueTrack")
    suspend fun _clearQueue()

    @Insert
    suspend fun _insertQueueTracks(vararg queueTracks: QueueTrack)

    @Query(
        """
        SELECT t.*, a.*, QueueTrack_uri, QueueTrack_queueTrackId, QueueTrack_position 
        FROM QueueTrack qt 
        JOIN Track t ON Track_trackId = QueueTrack_trackId
        LEFT JOIN Album a ON Track_albumId = Album_albumId
        ORDER BY QueueTrack_position, QueueTrack_queueTrackId
        """
    )
    @Transaction
    suspend fun getQueue(): List<QueueTrackPojo>

    @Transaction
    suspend fun setQueue(queue: List<QueueTrackPojo>) {
        _clearQueue()
        _insertQueueTracks(*queue.map { it.queueTrack }.toTypedArray())
    }
}
