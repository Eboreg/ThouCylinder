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

    @Query("SELECT Track.* FROM QueueTrack JOIN Track ON Track_trackId = QueueTrack_trackId")
    fun flowTracksInQueue(): Flow<List<Track>>

    @Query(
        """
        SELECT Track.*, Album.*, SpotifyTrack.*, LastFmTrack.*, SpotifyAlbum.*, QueueTrack_uri, QueueTrack_queueTrackId, QueueTrack_position 
        FROM QueueTrack  
            JOIN Track ON Track_trackId = QueueTrack_trackId
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
            LEFT JOIN LastFmTrack ON Track_trackId = LastFmTrack_trackId
            LEFT JOIN SpotifyAlbum ON Track_albumId = SpotifyAlbum_albumId
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
