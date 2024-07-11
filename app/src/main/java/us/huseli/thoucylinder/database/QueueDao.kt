@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.track.QueueTrack
import us.huseli.thoucylinder.dataclasses.track.Track
import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo
import us.huseli.thoucylinder.interfaces.ILogger

@Dao
abstract class QueueDao : ILogger {
    @Insert
    protected abstract suspend fun _insertQueueTracks(vararg queueTracks: QueueTrack)

    @Query("SELECT QueueTrack_queueTrackId FROM QueueTrack")
    protected abstract suspend fun _listQueueTrackIds(): List<String>

    @Update
    protected abstract suspend fun _updateQueueTracks(vararg queueTracks: QueueTrack)

    @Query("DELETE FROM QueueTrack WHERE QueueTrack_queueTrackId IN (:queueTrackIds)")
    abstract suspend fun deleteQueueTracks(vararg queueTrackIds: String)

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
                        logError("_updateQueueTracks($it): $e", e)
                    }
                }
                toInsert.forEach {
                    try {
                        _insertQueueTracks(it)
                    } catch (e: Exception) {
                        logError("_insertQueueTracks($it): $e", e)
                    }
                }
            }
        }
    }
}
