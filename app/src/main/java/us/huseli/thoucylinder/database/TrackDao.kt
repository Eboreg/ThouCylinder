@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.net.Uri
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

@Dao
interface TrackDao {
    /** Pseudo-private methods ***********************************************/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertTracks(vararg tracks: Track)

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId 
        WHERE Track_isInLibrary = 1 AND LOWER(Track_title) >= LOWER(:fromTitle) AND LOWER(Track_title) <= LOWER(:toTitle)  
        ORDER BY LOWER(Track_title)
        """
    )
    suspend fun _listTrackPojosBetween(fromTitle: String, toTitle: String): List<TrackPojo>

    @RawQuery(observedEntities = [Track::class, Album::class])
    fun _searchTracks(query: SupportSQLiteQuery): PagingSource<Int, TrackPojo>

    @Update
    suspend fun _updateTracks(vararg tracks: Track)

    /** Public methods *******************************************************/

    @Query("DELETE FROM Track")
    suspend fun clearTracks()

    @Delete
    suspend fun deleteTracks(vararg tracks: Track)

    @Query("DELETE FROM Track WHERE Track_isInLibrary = 0")
    suspend fun deleteTempTracks()

    @Query("DELETE FROM Track WHERE Track_albumId = :albumId")
    suspend fun deleteTracksByAlbumId(albumId: UUID)

    suspend fun insertTracks(tracks: List<Track>) =
        _insertTracks(*tracks.map { it.copy(isInLibrary = true) }.toTypedArray())

    suspend fun insertTempTracks(tracks: List<Track>) =
        _insertTracks(*tracks.map { it.copy(isInLibrary = false) }.toTypedArray())

    @Query(
        """
        SELECT Track_image_uri FROM Track WHERE Track_image_uri IS NOT NULL
        UNION
        SELECT Track_image_thumbnailUri FROM Track WHERE Track_image_thumbnailUri IS NOT NULL
        """
    )
    suspend fun listImageUris(): List<Uri>

    @Query("SELECT * FROM Track WHERE Track_isInLibrary = 1")
    suspend fun listTracks(): List<Track>

    suspend fun listTracksBetween(from: AbstractTrackPojo, to: AbstractTrackPojo): List<TrackPojo> {
        if (from.track.title.lowercase() < to.track.title.lowercase())
            return _listTrackPojosBetween(from.track.title, to.track.title)
        return _listTrackPojosBetween(to.track.title, from.track.title)
    }

    @Transaction
    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId 
        WHERE Track_isInLibrary = 1 ORDER BY LOWER(Track_title)
        """
    )
    fun pageTrackPojos(): PagingSource<Int, TrackPojo>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE (
            LOWER(Track_artist) = LOWER(:artist) OR (Track_artist IS NULL AND LOWER(Album_artist) = LOWER(:artist))
        ) AND Track_isInLibrary = 1
        ORDER BY LOWER(Track_title)
        """
    )
    @Transaction
    fun pageTracksByArtist(artist: String): PagingSource<Int, TrackPojo>

    fun searchTracks(query: String): PagingSource<Int, TrackPojo> {
        val terms = query.trim().split(Regex("\\s+")).filter { it.length > 2 }
            .joinToString(" OR ") {
                "Track_title LIKE '%$it%' OR Track_artist LIKE '%$it%' OR Album_title LIKE '%$it%' OR Album_artist LIKE '%$it%'"
            }

        return _searchTracks(
            SimpleSQLiteQuery(
                """
                SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId
                WHERE ($terms) AND Track_isInLibrary = 1
                """.trimIndent()
            )
        )
    }

    suspend fun updateTracks(vararg tracks: Track) =
        _updateTracks(*tracks.map { it.copy(isInLibrary = true) }.toTypedArray())
}
