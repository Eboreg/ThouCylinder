@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.entities.QueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
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

    @Query("SELECT Track.* FROM QueueTrack JOIN Track ON Track_trackId = QueueTrack_trackId")
    fun flowTracksInQueue(): Flow<List<Track>>

    @Transaction
    @Query(
        """
        SELECT Track.*, Album.*, QueueTrack_uri, QueueTrack_queueTrackId, QueueTrack_position,
            GROUP_CONCAT(AlbumArtist_name, '/') AS albumArtist            
        FROM QueueTrack  
            JOIN Track ON Track_trackId = QueueTrack_trackId
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN AlbumArtistCredit ON Album_albumId = AlbumArtist_albumId
        GROUP BY QueueTrack_queueTrackId
        ORDER BY QueueTrack_position, QueueTrack_queueTrackId
        """
    )
    suspend fun getQueue(): List<QueueTrackCombo>

    @Transaction
    suspend fun upsertQueueTracks(vararg queueTracks: QueueTrack) {
        if (queueTracks.isNotEmpty()) {
            val ids = _listQueueTrackIds()
            queueTracks.partition { ids.contains(it.queueTrackId) }.also { (toUpdate, toInsert) ->
                _updateQueueTracks(*toUpdate.toTypedArray())
                _insertQueueTracks(*toInsert.toTypedArray())
            }
        }
    }
}
