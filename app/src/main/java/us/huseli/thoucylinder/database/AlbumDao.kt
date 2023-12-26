@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import android.database.DatabaseUtils
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
import us.huseli.thoucylinder.AlbumSortParameter
import us.huseli.thoucylinder.SortOrder
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

@Dao
interface AlbumDao {
    /** Pseudo-private methods ************************************************/
    @RawQuery(
        observedEntities = [
            Album::class,
            Track::class,
            Genre::class,
            Style::class,
            SpotifyAlbum::class,
            LastFmAlbum::class,
        ],
    )
    fun _flowAlbumPojos(query: SupportSQLiteQuery): Flow<List<AlbumPojo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumGenres(vararg albumGenres: AlbumGenre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumStyles(vararg albumStyles: AlbumStyle)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertGenres(vararg genres: Genre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertStyles(vararg styles: Style)

    /** Public methods ********************************************************/
    @Query("SELECT EXISTS(SELECT Album_albumId FROM Album WHERE Album_albumId = :albumId)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM AlbumGenre WHERE AlbumGenre_albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE AlbumStyle_albumId = :albumId")
    suspend fun clearAlbumStyles(albumId: UUID)

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isDeleted = 1")
    suspend fun deleteMarkedAlbums()

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    suspend fun deleteTempAlbums()

    @Query("DELETE FROM Track WHERE Track_albumId IN (SELECT Album_albumId FROM Album WHERE Album_isDeleted = 1)")
    suspend fun deleteTracksFromMarkedAlbums()

    fun flowAlbumPojosByArtist(artist: String) = flowAlbumPojos(
        sortParameter = AlbumSortParameter.TITLE,
        sortOrder = SortOrder.ASCENDING,
        artist = artist,
    )

    fun flowAlbumPojos(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        artist: String? = null,
        searchTerm: String? = null,
    ): Flow<List<AlbumPojo>> {
        val searchQuery = searchTerm
            ?.lowercase()
            ?.split(Regex(" +"))
            ?.takeIf { it.isNotEmpty() }
            ?.map { DatabaseUtils.sqlEscapeString("%$it%") }
            ?.joinToString(" AND ") { term ->
                "(LOWER(Album_artist) LIKE $term OR LOWER(Album_title) LIKE $term OR Album_year LIKE $term)"
            }

        return _flowAlbumPojos(
            SimpleSQLiteQuery(
                """
                SELECT Album.*, 
                    SUM(COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs, Track_youtubeVideo_metadata_durationMs)) AS durationMs,
                    MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount,
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NOT NULL) AND 
                    EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL) AS isPartiallyDownloaded
                FROM Album LEFT JOIN Track ON Album_albumId = Track_albumId 
                WHERE Album_isInLibrary = 1 AND Album_isDeleted = 0 AND Album_isHidden = 0
                    ${artist?.let { "AND LOWER(Album_artist) = LOWER(\"$it\")" } ?: ""}
                    ${searchQuery?.let { "AND $it" } ?: ""}                                
                GROUP BY Album_albumId
                ORDER BY ${sortParameter.sqlColumn} ${if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"}
                """.trimIndent()
            )
        )
    }

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    suspend fun getAlbum(albumId: UUID): Album?

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo?

    @Transaction
    suspend fun insertAlbumGenres(pojo: AbstractAlbumPojo) {
        _insertGenres(*pojo.genres.toTypedArray())
        _insertAlbumGenres(*pojo.genres.map {
            AlbumGenre(albumId = pojo.album.albumId, genreName = it.genreName)
        }.toTypedArray())
    }

    @Transaction
    suspend fun insertAlbumStyles(pojo: AbstractAlbumPojo) {
        _insertStyles(*pojo.styles.toTypedArray())
        _insertAlbumStyles(*pojo.styles.map {
            AlbumStyle(albumId = pojo.album.albumId, styleName = it.styleName)
        }.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(vararg genres: Genre)

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query("SELECT * FROM Genre")
    suspend fun listGenres(): List<Genre>

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*, LastFmTrack.*
        FROM Track
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
            LEFT JOIN LastFmTrack ON Track_trackId = LastFmTrack_trackId
        WHERE Track_albumId IN (:albumIds)
        ORDER BY Track_albumId, Track_discNumber, Track_albumPosition
        """
    )
    suspend fun listTrackPojos(albumIds: List<UUID>): List<TrackPojo>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    suspend fun listTracks(albumId: UUID): List<Track>

    @Update
    suspend fun updateAlbums(vararg albums: Album)
}
