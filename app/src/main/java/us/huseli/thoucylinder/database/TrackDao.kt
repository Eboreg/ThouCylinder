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
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.TrackSortParameter
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

@Dao
interface TrackDao {
    @RawQuery(observedEntities = [Track::class, Album::class, SpotifyTrack::class, LastFmTrack::class])
    fun _pageTrackPojos(query: SupportSQLiteQuery): PagingSource<Int, TrackPojo>

    /** Public methods ********************************************************/
    @Delete
    suspend fun deleteTracks(vararg tracks: Track)

    @Query("DELETE FROM Track WHERE Track_isInLibrary = 0")
    suspend fun deleteTempTracks()

    @Query("DELETE FROM Track WHERE Track_albumId IN (:albumIds)")
    suspend fun deleteTracksByAlbumId(vararg albumIds: UUID)

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*, LastFmTrack.*
        FROM Track
            LEFT JOIN Album ON Track_albumId = Album_albumId 
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
            LEFT JOIN LastFmTrack ON Track_trackId = LastFmTrack_trackId
        WHERE Track_albumId = :albumId
        ORDER BY Track_albumPosition
        """
    )
    fun flowTrackPojosByAlbumId(albumId: UUID): Flow<List<TrackPojo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(vararg tracks: Track)

    @Query("SELECT * FROM Track WHERE Track_isInLibrary = 1")
    suspend fun listLibraryTracks(): List<Track>

    @Query("SELECT Track_localUri FROM Track WHERE Track_localUri IS NOT NULL")
    suspend fun listLocalUris(): List<Uri>

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
                SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*
                FROM Track
                    LEFT JOIN Album ON Track_albumId = Album_albumId
                    LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
                WHERE Track_isInLibrary = 1 AND Album_isHidden != 1 
                    ${searchQuery?.let { "AND $it" } ?: ""}
                ORDER BY ${sortParameter.sqlColumn} ${sortOrder.sql}
                """.trimIndent()
            )
        )
    }

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*, LastFmTrack.*
        FROM Track
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
            LEFT JOIN LastFmTrack ON Track_trackId = LastFmTrack_trackId
        WHERE (
            LOWER(Track_artist) = LOWER(:artist) OR (Track_artist IS NULL AND LOWER(Album_artist) = LOWER(:artist))
        ) AND Track_isInLibrary = 1
        ORDER BY LOWER(Track_title)
        """
    )
    @Transaction
    fun pageTrackPojosByArtist(artist: String): PagingSource<Int, TrackPojo>

    @Query("UPDATE Track SET Track_isInLibrary = :isInLibrary WHERE Track_albumId = :albumId")
    suspend fun setIsInLibraryByAlbumId(albumId: UUID, isInLibrary: Boolean)

    @Update
    suspend fun updateTracks(vararg tracks: Track)
}
