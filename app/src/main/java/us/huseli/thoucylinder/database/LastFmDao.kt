@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.pojos.LastFmAlbumPojo

@Dao
interface LastFmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertLastFmAlbum(album: LastFmAlbum)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertLastFmTracks(vararg tracks: LastFmTrack)

    @Transaction
    suspend fun insertLastFmAlbumPojo(pojo: LastFmAlbumPojo) {
        _insertLastFmAlbum(pojo.album)
        _insertLastFmTracks(*pojo.tracks.toTypedArray())
    }

    @Query(
        """
        SELECT LastFmAlbum_musicBrainzId FROM LastFmAlbum
            JOIN Album ON Album_albumId = LastFmAlbum_albumId
        WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0
        """
    )
    suspend fun listImportedAlbumIds(): List<String>
}
