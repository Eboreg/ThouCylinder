@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.util.UUID

@Dao
interface AlbumDao {
    /** Pseudo-private methods ***********************************************/
    @Insert
    suspend fun _insertAlbums(vararg albums: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumGenres(vararg albumGenres: AlbumGenre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertAlbumStyles(vararg albumStyles: AlbumStyle)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertGenres(vararg genres: Genre)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertStyles(vararg styles: Style)

    @RawQuery
    fun _searchAlbums(query: SupportSQLiteQuery): Flow<List<AlbumPojo>>

    @Update
    suspend fun _updateAlbums(vararg albums: Album)

    /** Public methods *******************************************************/
    @Query("SELECT EXISTS(SELECT Album_albumId FROM Album WHERE Album_albumId = :albumId)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM AlbumGenre WHERE AlbumGenre_albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE AlbumStyle_albumId = :albumId")
    suspend fun clearAlbumStyles(albumId: UUID)

    @Query("DELETE FROM Album")
    suspend fun clearAlbums()

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("DELETE FROM Album WHERE Album_isInLibrary = 0")
    suspend fun deleteTempAlbums()

    @Query("DELETE FROM Track WHERE Track_albumId = :albumId")
    suspend fun deleteTracks(albumId: UUID)

    @Query(
        """
        SELECT a.*, SUM(Track_metadata_durationMs) AS durationMs, MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear,
            COUNT(Track_trackId) AS trackCount
        FROM Album a LEFT JOIN Track t ON Album_albumId = Track_albumId 
        WHERE Album_isInLibrary = 1
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_artist), LOWER(Album_title)
        """
    )
    @Transaction
    fun flowAlbumPojos(): Flow<List<AlbumPojo>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    suspend fun getAlbum(albumId: UUID): Album?

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId")
    @Transaction
    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo?

    suspend fun insertAlbumGenres(pojo: AbstractAlbumPojo) {
        _insertGenres(*pojo.genres.toTypedArray())
        _insertAlbumGenres(*pojo.genres.map {
            AlbumGenre(albumId = pojo.album.albumId, genreName = it.genreName)
        }.toTypedArray())
    }

    suspend fun insertAlbumStyles(pojo: AbstractAlbumPojo) {
        _insertStyles(*pojo.styles.toTypedArray())
        _insertAlbumStyles(*pojo.styles.map {
            AlbumStyle(albumId = pojo.album.albumId, styleName = it.styleName)
        }.toTypedArray())
    }

    suspend fun insertAlbum(album: Album) = _insertAlbums(album.copy(isInLibrary = true))

    suspend fun insertTempAlbum(album: Album) = _insertAlbums(album.copy(isInLibrary = false))

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE Track_albumId IN (:albumIds)
        ORDER BY Track_albumId, Track_discNumber, Track_albumPosition
        """
    )
    suspend fun listTrackPojos(albumIds: List<UUID>): List<TrackPojo>

    @Query("SELECT * FROM Track WHERE Track_albumId = :albumId ORDER BY Track_discNumber, Track_albumPosition")
    suspend fun listTracks(albumId: UUID): List<Track>

    fun searchAlbums(query: String): Flow<List<AlbumPojo>> {
        val terms = query.trim().split(Regex("\\s+")).filter { it.length > 2 }
            .joinToString(" OR ") { "Album_title LIKE '%$it%' OR Album_artist LIKE '%$it%'" }

        return _searchAlbums(
            SimpleSQLiteQuery(
                """
                SELECT a.*, SUM(Track_metadata_durationMs) AS durationMs, MIN(Track_year) AS minYear,
                    MAX(Track_year) AS maxYear, COUNT(Track_trackId) AS trackCount
                FROM Album a LEFT JOIN Track t ON Album_albumId = Track_albumId
                WHERE ($terms) AND Album_isInLibrary = 1
                GROUP BY Album_albumId
                ORDER BY LOWER(Album_artist), LOWER(Album_title)
                """.trimIndent()
            )
        )
    }

    suspend fun updateAlbum(album: Album) {
        _updateAlbums(album.copy(isInLibrary = true))
    }
}
