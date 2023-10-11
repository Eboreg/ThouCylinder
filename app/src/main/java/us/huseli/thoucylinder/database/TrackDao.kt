@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID

@Dao
interface TrackDao {
    /** Pseudo-private methods ***********************************************/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertTracks(vararg tracks: Track)

    /** Public methods *******************************************************/
    @Query("DELETE FROM Track")
    suspend fun clearTracks()

    @Delete
    suspend fun deleteTracks(vararg tracks: Track)

    @Query("DELETE FROM Track WHERE Track_albumId = :albumId")
    suspend fun deleteTracksByAlbumId(albumId: UUID)

    suspend fun insertTracks(vararg tracks: Track) =
        _insertTracks(*tracks.map { it.copy(isInLibrary = true) }.toTypedArray())

    @Query("SELECT * FROM Track WHERE Track_isInLibrary = 1")
    suspend fun listTracks(): List<Track>

    @Transaction
    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId 
        AND Track_isInLibrary = 1 ORDER BY LOWER(Track_title)
        """
    )
    fun pageTrackPojos(): PagingSource<Int, TrackPojo>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE (Track_artist = :artist OR (Track_artist IS NULL AND Album_artist = :artist)) AND Track_isInLibrary = 1
        ORDER BY LOWER(Track_title)
        """
    )
    @Transaction
    fun pageTracksByArtist(artist: String): PagingSource<Int, TrackPojo>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t 
        LEFT JOIN Album a ON Track_albumId = Album_albumId
        JOIN PlaylistTrack pt ON Track_trackId = PlaylistTrack_trackId 
        AND PlaylistTrack_playlistId = :playlistId AND Track_isInLibrary = 1
        """
    )
    @Transaction
    fun pageTracksByPlaylistId(playlistId: UUID): PagingSource<Int, TrackPojo>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE (Track_title LIKE :query OR Track_artist LIKE :query) AND Track_isInLibrary = 1
        """
    )
    @Transaction
    fun simpleTrackSearch(query: String): PagingSource<Int, TrackPojo>
}
