package us.huseli.thoucylinder.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.Track

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg tracks: Track)

    @Query("SELECT * FROM Track")
    fun list(): Flow<List<Track>>

    @Query("SELECT * FROM Track WHERE albumId IS NULL")
    fun listSingle(): Flow<List<Track>>
}