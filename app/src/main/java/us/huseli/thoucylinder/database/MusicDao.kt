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
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.AlbumGenre
import us.huseli.thoucylinder.dataclasses.AlbumStyle
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

    @Delete
    suspend fun _deleteAlbums(vararg albums: Album)

    @Delete
    suspend fun _deleteTracks(vararg tracks: Track)

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

    @Transaction
    suspend fun deleteAlbumWithTracks(album: Album) {
        _deleteTracks(*album.tracks.toTypedArray())
        _deleteAlbums(album)
    }

    @Transaction
    suspend fun deleteAll() {
        _clearTracks()
        _clearAlbums()
    }

    @Query("DELETE FROM Track WHERE albumId = :albumId")
    suspend fun deleteTracksByAlbumId(albumId: UUID)

    suspend fun insertTrack(track: Track) = _insertTracks(track.copy(isInLibrary = true))

    @Query("SELECT * FROM AlbumGenre")
    fun listAbumGenres(): Flow<List<AlbumGenre>>

    @Query("SELECT * FROM AlbumStyle")
    fun listAlbumStyles(): Flow<List<AlbumStyle>>

    @Query("SELECT * FROM Album LEFT JOIN Track ON Album.albumId = Track.albumId")
    fun listAlbumsWithTracks(): Flow<Map<Album, List<Track>>>

    @Query("SELECT * FROM Track WHERE albumId IS NULL")
    fun listSingleTracks(): Flow<List<Track>>

    @Query("SELECT * FROM Track")
    fun listTracks(): Flow<List<Track>>

    @Transaction
    suspend fun upsertAlbumWithTracks(album: Album) {
        val genres = album.genres.map { Genre(genreId = it) }
        val styles = album.styles.map { Style(styleId = it) }

        deleteTracksByAlbumId(album.albumId)
        if (albumExists(album.albumId)) _updateAlbums(album.copy(isInLibrary = true))
        else _insertAlbums(album.copy(isInLibrary = true))
        _insertTracks(*album.tracks.map { it.copy(isInLibrary = true, albumId = album.albumId) }.toTypedArray())
        _clearAlbumGenres(album.albumId)
        _clearAlbumStyles(album.albumId)
        _insertGenres(*genres.toTypedArray())
        _insertStyles(*styles.toTypedArray())
        _insertAlbumGenres(*genres.map { AlbumGenre(albumId = album.albumId, genreId = it.genreId) }.toTypedArray())
        _insertAlbumStyles(*styles.map { AlbumStyle(albumId = album.albumId, styleId = it.styleId) }.toTypedArray())
    }
}
