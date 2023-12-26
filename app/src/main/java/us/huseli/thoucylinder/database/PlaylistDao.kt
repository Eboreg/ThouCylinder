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

@Dao
interface PlaylistDao {
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query(
        """
        DELETE FROM PlaylistTrack
        WHERE PlaylistTrack_trackId NOT IN (SELECT Track_trackId FROM Track)
            OR PlaylistTrack_playlistId NOT IN (SELECT Playlist_playlistId FROM Playlist)
        """
    )
    suspend fun deleteOrphanPlaylistTracks()

    @Delete
    suspend fun deletePlaylistTracks(vararg tracks: PlaylistTrack)

    @Query(
        """
        SELECT Playlist.*, COUNT(PlaylistTrack_trackId) AS trackCount, SUM(Track_metadata_durationMs) AS totalDurationMs
        FROM Playlist 
            LEFT JOIN PlaylistTrack ON Playlist_playlistId = PlaylistTrack_playlistId 
            LEFT JOIN Track ON PlaylistTrack_trackId = Track_trackId
        GROUP BY Playlist_playlistId
        HAVING Playlist_playlistId IS NOT NULL
        """
    )
    fun flowPojos(): Flow<List<PlaylistPojo>>

    @Insert
    suspend fun insertPlaylists(vararg playlists: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(vararg playlistTracks: PlaylistTrack)

    @Query(
        """
        SELECT DISTINCT Album.* 
        FROM Album
            JOIN Track ON Track_albumId = Album_albumId
            JOIN PlaylistTrack ON PlaylistTrack_trackId = Track_trackId 
        WHERE PlaylistTrack_playlistId = :playlistId
        """
    )
    suspend fun listAlbums(playlistId: UUID): List<Album>

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*, LastFmTrack.*, Playlist.*, PlaylistTrack.PlaylistTrack_position
        FROM Track
            JOIN PlaylistTrack ON Track_trackId = PlaylistTrack_trackId 
            JOIN Playlist ON PlaylistTrack_playlistId = Playlist_playlistId
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
            LEFT JOIN LastFmTrack ON Track_trackId = LastFmTrack_trackId
        WHERE Playlist_playlistId = :playlistId
        ORDER BY PlaylistTrack_position
        """
    )
    suspend fun listTracks(playlistId: UUID): List<PlaylistTrackPojo>

    @Query(
        """
        SELECT DISTINCT Track.*, Album.*, SpotifyTrack.*, LastFmTrack.*, Playlist.*, PlaylistTrack.PlaylistTrack_position
        FROM Track 
            JOIN PlaylistTrack ON Track_trackId = PlaylistTrack_trackId 
            JOIN Playlist ON PlaylistTrack_playlistId = Playlist_playlistId 
            LEFT JOIN Album ON Track_albumId = Album_albumId
            LEFT JOIN SpotifyTrack ON Track_trackId = SpotifyTrack_trackId
            LEFT JOIN LastFmTrack ON Track_trackId = LastFmTrack_trackId
        WHERE Playlist_playlistId = :playlistId
        ORDER BY PlaylistTrack_position
        """
    )
    @Transaction
    fun pageTracks(playlistId: UUID): PagingSource<Int, PlaylistTrackPojo>

    @Query("UPDATE Playlist SET Playlist_updated = :updated WHERE Playlist_playlistId = :playlistId")
    suspend fun touchPlaylist(playlistId: UUID, updated: Instant = Instant.now())
}
