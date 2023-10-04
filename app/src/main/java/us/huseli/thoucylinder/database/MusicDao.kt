@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.AlbumGenre
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumStyle
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.ArtistPojo
import us.huseli.thoucylinder.dataclasses.Genre
import us.huseli.thoucylinder.dataclasses.Style
import us.huseli.thoucylinder.dataclasses.Track
import java.util.UUID

@Dao
interface MusicDao {
    /** Pseudo-private methods ***********************************************/
    @Query("DELETE FROM Album")
    suspend fun _clearAlbums()

    @Query("DELETE FROM AlbumGenre WHERE albumId = :albumId")
    suspend fun _clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE albumId = :albumId")
    suspend fun _clearAlbumStyles(albumId: UUID)

    @Query("DELETE FROM Track")
    suspend fun _clearTracks()

    @Query("DELETE FROM Track WHERE albumId = :albumId")
    suspend fun _deleteTracksByAlbumId(albumId: UUID)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertTracks(vararg tracks: Track)

    @Update
    suspend fun _updateAlbums(vararg albums: Album)

    /** Public methods *******************************************************/
    @Query("SELECT EXISTS(SELECT albumId FROM Album WHERE albumId = :albumId)")
    suspend fun albumExists(albumId: UUID): Boolean

    @Delete
    suspend fun deleteAlbums(vararg albums: Album)

    @Transaction
    suspend fun deleteAlbumWithTracks(pojo: AlbumWithTracksPojo) {
        deleteTracks(*pojo.tracks.toTypedArray())
        deleteAlbums(pojo.album)
    }

    @Transaction
    suspend fun deleteAll() {
        _clearTracks()
        _clearAlbums()
    }

    @Delete
    suspend fun deleteTracks(vararg tracks: Track)

    suspend fun insertTrack(track: Track) {
        _insertTracks(track.copy(isInLibrary = true))
    }

    @Query("SELECT * FROM Album WHERE albumId = :albumId")
    @Transaction
    fun flowAlbumWithSongs(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query(
        """
        SELECT a.*, SUM(t.metadatadurationMs) AS durationMs, MIN(t.year) AS minYear, MAX(t.year) AS maxYear,
            COUNT(t.id) AS trackCount
        FROM Album a LEFT JOIN Track t ON a.albumId = t.albumId
        GROUP BY a.albumId
        ORDER BY LOWER(a.artist), LOWER(a.title)
        """
    )
    @Transaction
    fun flowAlbumPojos(): Flow<List<AlbumPojo>>

    @Query(
        """
        SELECT
            COALESCE(t.artist, a.artist) AS name,
            COUNT(DISTINCT t.id) AS trackCount,
            (SELECT COUNT(*) FROM Album a2 WHERE a2.artist = COALESCE(t.artist, a.artist)) AS albumCount,
            a3.albumArtlocalFile AS firstAlbumArt,
            COALESCE(SUM(t.metadatadurationMs), 0) AS totalDurationMs
        FROM Track t
            LEFT JOIN Album a ON t.albumId = a.albumId
            LEFT JOIN Album a3 ON a3.albumId = t.albumId AND a3.albumArtlocalFile IS NOT NULL
        WHERE name IS NOT NULL
        GROUP BY name
        ORDER BY LOWER(name)
        """
    )
    fun flowArtistPojos(): Flow<List<ArtistPojo>>

    @Query("SELECT * FROM Album")
    suspend fun listAlbums(): List<Album>

    @Query("SELECT * FROM Track")
    suspend fun listTracks(): List<Track>

    @Query("SELECT * FROM Track ORDER BY LOWER(title)")
    fun pageTracks(): PagingSource<Int, Track>

    @Query(
        """
        SELECT t.* FROM Track t LEFT JOIN Album a ON t.albumId = a.albumId
        WHERE t.artist = :artist OR (t.artist IS NULL AND a.artist = :artist)
        ORDER BY LOWER(t.title)
        """
    )
    fun pageTracksByArtist(artist: String): PagingSource<Int, Track>

    @Transaction
    suspend fun upsertAlbumWithTracks(pojo: AlbumWithTracksPojo) {
        if (albumExists(pojo.album.albumId)) {
            _updateAlbums(pojo.album.copy(isInLibrary = true))
            _deleteTracksByAlbumId(pojo.album.albumId)
            _clearAlbumGenres(pojo.album.albumId)
            _clearAlbumStyles(pojo.album.albumId)
        } else _insertAlbums(pojo.album.copy(isInLibrary = true))

        if (pojo.tracks.isNotEmpty())
            _insertTracks(*pojo.tracks.map { it.copy(isInLibrary = true, albumId = pojo.album.albumId) }.toTypedArray())
        if (pojo.genres.isNotEmpty()) {
            _insertGenres(*pojo.genres.toTypedArray())
            _insertAlbumGenres(*pojo.genres.map {
                AlbumGenre(albumId = pojo.album.albumId, genreId = it.genreId)
            }.toTypedArray())
        }
        if (pojo.styles.isNotEmpty()) {
            _insertStyles(*pojo.styles.toTypedArray())
            _insertAlbumStyles(*pojo.styles.map {
                AlbumStyle(albumId = pojo.album.albumId, styleId = it.styleId)
            }.toTypedArray())
        }
    }
}
