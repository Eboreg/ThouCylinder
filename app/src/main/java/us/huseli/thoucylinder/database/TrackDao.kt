@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM Track WHERE albumId = :albumId")
    suspend fun deleteTracksByAlbumId(albumId: UUID)

    suspend fun insertTracks(vararg tracks: Track) =
        _insertTracks(*tracks.map { it.copy(isInLibrary = true) }.toTypedArray())

    @Query("SELECT * FROM Track WHERE isInLibrary = 1")
    suspend fun listTracks(): List<Track>

    @Query("SELECT * FROM Track WHERE isInLibrary = 1 ORDER BY LOWER(title)")
    fun pageTracks(): PagingSource<Int, Track>

    @Query(
        """
        SELECT t.* FROM Track t LEFT JOIN Album a ON t.albumId = a.albumId
        WHERE (t.artist = :artist OR (t.artist IS NULL AND a.artist = :artist)) AND t.isInLibrary = 1
        ORDER BY LOWER(t.title)
        """
    )
    fun pageTracksByArtist(artist: String): PagingSource<Int, Track>

    @Query("SELECT t.* FROM Track t JOIN PlaylistTrack pt ON t.trackId = pt.trackId AND pt.playlistId = :playlistId AND t.isInLibrary = 1")
    fun pageTracksByPlaylistId(playlistId: UUID): PagingSource<Int, Track>

    @Query("SELECT * FROM Track WHERE (title LIKE :query OR artist LIKE :query) AND isInLibrary = 1")
    fun simpleTrackSearch(query: String): PagingSource<Int, Track>

}