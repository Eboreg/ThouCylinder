@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import java.time.Instant
import java.util.UUID

@Dao
interface PlaylistDao {
    /** Pseudo-private methods ***********************************************/
    @Query("DELETE FROM PlaylistTrack WHERE PlaylistTrack_playlistId = :playlistId")
    suspend fun _clearPlaylistTracks(playlistId: UUID)

    @Insert
    suspend fun _insertPlaylists(vararg playlists: Playlist)

    @Query("SELECT EXISTS(SELECT Playlist_playlistId FROM Playlist WHERE Playlist_playlistId = :playlistId)")
    suspend fun _playlistExists(playlistId: UUID): Boolean

    @Update
    suspend fun _updatePlaylists(vararg playlist: Playlist)

    /** Public methods *******************************************************/
    @Query(
        """
        SELECT p.*, COUNT(PlaylistTrack_trackId) AS trackCount, SUM(Track_metadata_durationMs) AS totalDurationMs
        FROM Playlist p 
            LEFT JOIN PlaylistTrack pt ON Playlist_playlistId = PlaylistTrack_playlistId 
            LEFT JOIN Track t ON PlaylistTrack_trackId = Track_trackId
        GROUP BY Playlist_playlistId
        HAVING Playlist_playlistId IS NOT NULL
        """
    )
    fun flowPojos(): Flow<List<PlaylistPojo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(vararg playlistTracks: PlaylistTrack)

    @Query(
        """
        SELECT DISTINCT a.* FROM Album a
        JOIN Track t ON t.Track_albumId = a.Album_albumId
        JOIN PlaylistTrack pt ON pt.PlaylistTrack_trackId = t.Track_trackId 
            AND pt.PlaylistTrack_playlistId = :playlistId
        """
    )
    suspend fun listAlbums(playlistId: UUID): List<Album>

    @Query(
        """
        SELECT DISTINCT t.* FROM Track t
        JOIN PlaylistTrack pt ON pt.PlaylistTrack_trackId = t.Track_trackId
            AND pt.PlaylistTrack_playlistId = :playlistId
        ORDER BY pt.PlaylistTrack_position
        """
    )
    suspend fun listTracks(playlistId: UUID): List<Track>

    @Query(
        """
        SELECT DISTINCT t.*, a.* FROM Track t 
        LEFT JOIN Album a ON Track_albumId = Album_albumId
        JOIN PlaylistTrack pt ON t.Track_trackId = pt.PlaylistTrack_trackId 
            AND pt.PlaylistTrack_playlistId = :playlistId AND t.Track_isInLibrary = 1
        ORDER BY pt.PlaylistTrack_position
        """
    )
    @Transaction
    fun pageTracks(playlistId: UUID): PagingSource<Int, TrackPojo>

    suspend fun upsertPlaylistWithTracks(playlist: Playlist, tracks: List<PlaylistTrack>) {
        val now = Instant.now()

        if (_playlistExists(playlist.playlistId)) {
            _updatePlaylists(playlist.copy(updated = now))
            _clearPlaylistTracks(playlist.playlistId)
        } else {
            _insertPlaylists(playlist.copy(created = now, updated = now))
        }
        insertTracks(*tracks.toTypedArray())
    }
}
