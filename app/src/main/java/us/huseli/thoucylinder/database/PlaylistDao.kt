@file:Suppress("FunctionName")

package us.huseli.thoucylinder.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@Dao
interface PlaylistDao {
    @Insert
    suspend fun _insertPlaylists(vararg playlists: Playlist)

    @Query(
        """
        SELECT DISTINCT t.*, a.*, p.*, pt.PlaylistTrack_position FROM Track t
        JOIN PlaylistTrack pt ON t.Track_trackId = pt.PlaylistTrack_trackId 
        JOIN Playlist p ON pt.PlaylistTrack_playlistId = p.Playlist_playlistId
        LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE p.Playlist_playlistId = :playlistId 
            AND pt.PlaylistTrack_position >= :fromPosition
            AND pt.PlaylistTrack_position <= :toPosition
        ORDER BY pt.PlaylistTrack_position
        """
    )
    suspend fun _listTrackPojosBetween(playlistId: UUID, fromPosition: Int, toPosition: Int): List<PlaylistTrackPojo>

    /** Public methods *******************************************************/
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylistTracks(vararg tracks: PlaylistTrack)

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

    suspend fun insertPlaylist(playlist: Playlist) {
        val now = Instant.now()
        _insertPlaylists(playlist.copy(created = now, updated = now))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(vararg playlistTracks: PlaylistTrack)

    @Query(
        """
        SELECT DISTINCT a.* FROM Album a
        JOIN Track t ON t.Track_albumId = a.Album_albumId
        JOIN PlaylistTrack pt ON pt.PlaylistTrack_trackId = t.Track_trackId 
        WHERE pt.PlaylistTrack_playlistId = :playlistId
        """
    )
    suspend fun listAlbums(playlistId: UUID): List<Album>

    @Query(
        """
        SELECT DISTINCT t.*, a.*, p.*, pt.PlaylistTrack_position FROM Track t
        JOIN PlaylistTrack pt ON t.Track_trackId = pt.PlaylistTrack_trackId 
        JOIN Playlist p ON pt.PlaylistTrack_playlistId = p.Playlist_playlistId
        LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE p.Playlist_playlistId = :playlistId
        ORDER BY pt.PlaylistTrack_position
        """
    )
    suspend fun listTracks(playlistId: UUID): List<PlaylistTrackPojo>

    suspend fun listTracksBetween(
        playlistId: UUID,
        from: PlaylistTrackPojo,
        to: PlaylistTrackPojo,
    ): List<PlaylistTrackPojo> =
        _listTrackPojosBetween(playlistId, min(from.position, to.position), max(from.position, to.position))

    @Query(
        """
        SELECT DISTINCT t.*, a.*, p.*, pt.PlaylistTrack_position FROM Track t 
        JOIN PlaylistTrack pt ON t.Track_trackId = pt.PlaylistTrack_trackId 
        JOIN Playlist p ON pt.PlaylistTrack_playlistId = p.Playlist_playlistId 
        LEFT JOIN Album a ON Track_albumId = Album_albumId
        WHERE p.Playlist_playlistId = :playlistId
        ORDER BY pt.PlaylistTrack_position
        """
    )
    @Transaction
    fun pageTracks(playlistId: UUID): PagingSource<Int, PlaylistTrackPojo>

    @Query("UPDATE Playlist SET Playlist_updated = :updated WHERE Playlist_playlistId = :playlistId")
    suspend fun touchPlaylist(playlistId: UUID, updated: Instant = Instant.now())
}
