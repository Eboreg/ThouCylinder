package us.huseli.thoucylinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.data.entities.Track

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg tracks: Track)

    @Query("SELECT * FROM Track")
    fun list(): Flow<List<Track>>

    @Query("SELECT * FROM Track WHERE albumId IS NULL")
    fun listSingle(): Flow<List<Track>>
}