package us.huseli.thoucylinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.data.entities.Album
import us.huseli.thoucylinder.data.entities.Track

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(vararg tracks: Track)

    @Transaction
    suspend fun insertWithTracks(album: Album) {
        insert(album)
        insertTracks(*album.tracks.toTypedArray())
    }

    @Query("SELECT * FROM Album")
    fun list(): Flow<List<Album>>

    @Query("SELECT * FROM Album JOIN Track ON Album.id = Track.albumId")
    fun listWithTracks(): Flow<Map<Album, List<Track>>>
}
