@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
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
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

@Dao
interface TrackDao {
    /** Pseudo-private methods ************************************************/
    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId 
        WHERE Track_isInLibrary = 1 AND LOWER(Track_title) >= LOWER(:fromTitle) AND LOWER(Track_title) <= LOWER(:toTitle)  
        ORDER BY LOWER(Track_title)
        """
    )
    suspend fun _listTrackPojosBetween(fromTitle: String, toTitle: String): List<TrackPojo>

    @RawQuery(observedEntities = [Track::class, Album::class])
    fun _pageTrackPojos(query: SupportSQLiteQuery): PagingSource<Int, TrackPojo>

    @RawQuery(observedEntities = [Track::class, Album::class])
    fun _searchTrackPojos(query: SupportSQLiteQuery): PagingSource<Int, TrackPojo>

    /** Public methods ********************************************************/
    @Delete
    suspend fun deleteTracks(vararg tracks: Track)

    @Query("DELETE FROM Track WHERE Track_isInLibrary = 0")
    suspend fun deleteTempTracks()

    @Query("DELETE FROM Track WHERE Track_albumId = :albumId")
    suspend fun deleteTracksByAlbumId(albumId: UUID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(vararg tracks: Track)

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

    suspend fun listTrackPojosBetween(from: AbstractTrackPojo, to: AbstractTrackPojo): List<TrackPojo> {
        if (from.track.title.lowercase() < to.track.title.lowercase())
            return _listTrackPojosBetween(from.track.title, to.track.title)
        return _listTrackPojosBetween(to.track.title, from.track.title)
    }

    fun pageTrackPojos(
        sortParameter: TrackSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
    ): PagingSource<Int, TrackPojo> {
        val searchQuery = searchTerm
            .lowercase()
            .split(Regex(" +"))
            .takeIf { it.isNotEmpty() }
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(Track_title) LIKE $term OR LOWER(Album_title) LIKE $term OR " +
                    "LOWER(Track_artist) LIKE $term OR LOWER(Album_artist) LIKE $term OR " +
                    "Album_year LIKE $term OR Track_year LIKE $term)"
            }

        return _pageTrackPojos(
            SimpleSQLiteQuery(
                """
                SELECT DISTINCT Track.*, Album.* FROM Track LEFT JOIN Album ON Track_albumId = Album_albumId
                WHERE Track_isInLibrary = 1 ${searchQuery?.let { "AND $it" } ?: ""}
                ORDER BY ${sortParameter.sqlColumn} ${if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"}
                """.trimIndent()
            )
        )
    }

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
    fun pageTrackPojosByArtist(artist: String): PagingSource<Int, TrackPojo>

    @Update
    suspend fun updateTracks(vararg tracks: Track)
}
