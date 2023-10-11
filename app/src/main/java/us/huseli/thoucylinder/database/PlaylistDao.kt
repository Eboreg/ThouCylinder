@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
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
    fun flowPlaylists(): Flow<List<PlaylistPojo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTracks(vararg playlistTracks: PlaylistTrack)

    suspend fun upsertPlaylistWithTracks(playlist: Playlist, tracks: List<PlaylistTrack>) {
        val now = Instant.now()

        if (_playlistExists(playlist.playlistId)) {
            _updatePlaylists(playlist.copy(updated = now))
            _clearPlaylistTracks(playlist.playlistId)
        } else {
            _insertPlaylists(playlist.copy(created = now, updated = now))
        }
        insertPlaylistTracks(*tracks.toTypedArray())
    }
}
