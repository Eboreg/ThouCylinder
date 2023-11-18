@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import java.util.UUID

@Dao
interface QueueDao {
    @Insert
    suspend fun _insertQueueTracks(vararg queueTracks: QueueTrack)

    @Query("SELECT QueueTrack_queueTrackId FROM QueueTrack")
    suspend fun _listQueueTrackIds(): List<UUID>

    @Update
    suspend fun _updateQueueTracks(vararg queueTracks: QueueTrack)

    @Delete
    suspend fun deleteQueueTracks(vararg queueTracks: QueueTrack)

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
    suspend fun upsertQueueTracks(vararg queueTracks: QueueTrack) {
        val ids = _listQueueTrackIds()
        queueTracks.partition { ids.contains(it.queueTrackId) }.also { (toUpdate, toInsert) ->
            _updateQueueTracks(*toUpdate.toTypedArray())
            _insertQueueTracks(*toInsert.toTypedArray())
        }
    }
}
