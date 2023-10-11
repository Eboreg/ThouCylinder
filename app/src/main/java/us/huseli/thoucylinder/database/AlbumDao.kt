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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
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
    @Query("SELECT EXISTS(SELECT Album_albumId FROM Album WHERE Album_albumId = :albumId AND Album_isInLibrary = 1)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM AlbumGenre WHERE AlbumGenre_albumId = :albumId")
    suspend fun clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE AlbumStyle_albumId = :albumId")
    suspend fun clearAlbumStyles(albumId: UUID)

    @Query("DELETE FROM Album")
    suspend fun clearAlbums()

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId AND Album_isInLibrary = 1")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query(
        """
        SELECT a.*, SUM(Track_metadata_durationMs) AS durationMs, MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear,
            COUNT(Track_trackId) AS trackCount
        FROM Album a LEFT JOIN Track t ON Album_albumId = Track_albumId AND Track_isInLibrary = 1 AND Album_isInLibrary = 1
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_artist), LOWER(Album_title)
        """
    )
    @Transaction
    fun flowAlbumPojos(): Flow<List<AlbumPojo>>

    @Query("SELECT * FROM Album WHERE Album_albumId = :albumId AND Album_isInLibrary = 1")
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

    @Query("SELECT * FROM Album WHERE Album_isInLibrary = 1")
    suspend fun listAlbums(): List<Album>

    @Query(
        """
        SELECT a.*, SUM(Track_metadata_durationMs) AS durationMs, MIN(Track_year) AS minYear, MAX(Track_year) AS maxYear,
            COUNT(Track_trackId) AS trackCount
        FROM Album a LEFT JOIN Track t ON Album_albumId = Track_albumId
        WHERE (Album_title LIKE :query OR Album_artist LIKE :query) AND Album_isInLibrary = 1
        GROUP BY Album_albumId
        ORDER BY LOWER(Album_artist), LOWER(Album_title)
        """
    )
    @Transaction
    suspend fun simpleAlbumSearch(query: String): List<AlbumPojo>

    suspend fun updateAlbums(vararg albums: Album) =
        _updateAlbums(*albums.map { it.copy(isInLibrary = true) }.toTypedArray())
}
