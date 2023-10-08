@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.AlbumStyle
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style
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

    @Update
    suspend fun _updateAlbums(vararg albums: Album)

    /** Public methods *******************************************************/
    @Query("SELECT EXISTS(SELECT albumId FROM Album WHERE albumId = :albumId AND isInLibrary = 1)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM AlbumGenre WHERE albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE albumId = :albumId")
    suspend fun clearAlbumStyles(albumId: UUID)

    @Query("DELETE FROM Album")
    suspend fun clearAlbums()

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("SELECT * FROM Album WHERE albumId = :albumId AND isInLibrary = 1")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query(
        """
        SELECT a.*, SUM(t.metadatadurationMs) AS durationMs, MIN(t.year) AS minYear, MAX(t.year) AS maxYear,
            COUNT(t.trackId) AS trackCount
        FROM Album a LEFT JOIN Track t ON a.albumId = t.albumId AND t.isInLibrary = 1 AND a.isInLibrary = 1
        GROUP BY a.albumId
        ORDER BY LOWER(a.artist), LOWER(a.title)
        """
    )
    @Transaction
    fun flowAlbumPojos(): Flow<List<AlbumPojo>>

    @Query("SELECT * FROM Album WHERE albumId = :albumId AND isInLibrary = 1")
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

    suspend fun insertAlbums(vararg albums: Album) =
        _insertAlbums(*albums.map { it.copy(isInLibrary = true) }.toTypedArray())

    @Query("SELECT * FROM Album WHERE isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query(
        """
        SELECT a.*, SUM(t.metadatadurationMs) AS durationMs, MIN(t.year) AS minYear, MAX(t.year) AS maxYear,
            COUNT(t.trackId) AS trackCount
        FROM Album a LEFT JOIN Track t ON a.albumId = t.albumId
        WHERE (a.title LIKE :query OR a.artist LIKE :query) AND a.isInLibrary = 1
        GROUP BY a.albumId
        ORDER BY LOWER(a.artist), LOWER(a.title)
        """
    )
    @Transaction
    suspend fun simpleAlbumSearch(query: String): List<AlbumPojo>

    suspend fun updateAlbums(vararg albums: Album) =
        _updateAlbums(*albums.map { it.copy(isInLibrary = true) }.toTypedArray())
}
