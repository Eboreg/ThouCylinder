@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import java.util.UUID

@Dao
abstract class QueueDao {
    @Insert
    protected abstract suspend fun _insertQueueTracks(vararg queueTracks: QueueTrack)

    @Query("SELECT QueueTrack_queueTrackId FROM QueueTrack")
    protected abstract suspend fun _listQueueTrackIds(): List<UUID>

    @Update
    protected abstract suspend fun _updateQueueTracks(vararg queueTracks: QueueTrack)

    @Delete
    abstract suspend fun deleteQueueTracks(vararg queueTracks: QueueTrack)

    @Query("SELECT Track.* FROM QueueTrack JOIN Track ON Track_trackId = QueueTrack_trackId")
    abstract fun flowTracksInQueue(): Flow<List<Track>>

    @Transaction
    @Query("SELECT * FROM QueueTrackCombo")
    abstract suspend fun getQueue(): List<QueueTrackCombo>

    @Transaction
    open suspend fun upsertQueueTracks(vararg queueTracks: QueueTrack) {
        if (queueTracks.isNotEmpty()) {
            val ids = _listQueueTrackIds()
            queueTracks.partition { ids.contains(it.queueTrackId) }.also { (toUpdate, toInsert) ->
                toUpdate.forEach {
                    try {
                        _updateQueueTracks(it)
                    } catch (e: Exception) {
                        Log.e("QueueDao", "_updateQueueTracks($it): $e")
                    }
                }
                toInsert.forEach {
                    try {
                        _insertQueueTracks(it)
                    } catch (e: Exception) {
                        Log.e("QueueDao", "_insertQueueTracks($it): $e")
                    }
                }
            }
        }
    }
}
