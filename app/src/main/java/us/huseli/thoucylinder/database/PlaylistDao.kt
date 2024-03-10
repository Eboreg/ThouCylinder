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
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.combos.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.time.Instant
import java.util.UUID

@Dao
abstract class PlaylistDao {
    @Query("SELECT * FROM PlaylistTrack WHERE PlaylistTrack_playlistId = :playlistId ORDER BY PlaylistTrack_position")
    protected abstract suspend fun _listPlaylistTracks(playlistId: UUID): List<PlaylistTrack>

    @Update
    protected abstract suspend fun _updateTracks(vararg tracks: PlaylistTrack)

    @Delete
    abstract suspend fun deletePlaylist(playlist: Playlist)

    @Query(
        """
        DELETE FROM PlaylistTrack
        WHERE PlaylistTrack_trackId NOT IN (SELECT Track_trackId FROM Track)
            OR PlaylistTrack_playlistId NOT IN (SELECT Playlist_playlistId FROM Playlist)
        """
    )
    abstract suspend fun deleteOrphanPlaylistTracks()

    @Query("DELETE FROM PlaylistTrack WHERE PlaylistTrack_id IN (:ids)")
    abstract suspend fun deletePlaylistTracks(vararg ids: UUID)

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
    abstract fun flowPojos(): Flow<List<PlaylistPojo>>

    @Query("SELECT * FROM Playlist WHERE Playlist_playlistId = :playlistId")
    abstract fun flowPlaylist(playlistId: UUID): Flow<Playlist?>

    @Query("SELECT * FROM Playlist")
    abstract fun flowPlaylists(): Flow<List<Playlist>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT TrackCombo.*, Playlist.*, PlaylistTrack_position, PlaylistTrack_id
        FROM TrackCombo 
            JOIN PlaylistTrack ON Track_trackId = PlaylistTrack_trackId 
            JOIN Playlist ON PlaylistTrack_playlistId = Playlist_playlistId 
        WHERE Playlist_playlistId = :playlistId
        ORDER BY PlaylistTrack_position
        """
    )
    abstract fun flowTrackCombos(playlistId: UUID): Flow<List<PlaylistTrackCombo>>

    @Insert
    abstract suspend fun insertPlaylists(vararg playlists: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertPlaylistTracks(vararg playlistTracks: PlaylistTrack)

    @Query(
        """
        SELECT DISTINCT Album.* 
        FROM Album
            JOIN Track ON Track_albumId = Album_albumId
            JOIN PlaylistTrack ON PlaylistTrack_trackId = Track_trackId 
        WHERE PlaylistTrack_playlistId = :playlistId
        """
    )
    abstract suspend fun listAlbums(playlistId: UUID): List<Album>

    @Query("SELECT * FROM PlaylistTrack WHERE PlaylistTrack_playlistId = :playlistId")
    abstract suspend fun listPlaylistTracks(playlistId: UUID): List<PlaylistTrack>

    @Transaction
    @Query(
        """
        SELECT DISTINCT TrackCombo.*, Playlist.*, PlaylistTrack_position, PlaylistTrack_id
        FROM TrackCombo 
            JOIN PlaylistTrack ON Track_trackId = PlaylistTrack_trackId 
            JOIN Playlist ON PlaylistTrack_playlistId = Playlist_playlistId 
        WHERE Playlist_playlistId = :playlistId
        GROUP BY PlaylistTrack_id
        ORDER BY PlaylistTrack_position
        """
    )
    abstract suspend fun listTrackCombos(playlistId: UUID): List<PlaylistTrackCombo>

    @Transaction
    @Query(
        """
        SELECT DISTINCT TrackCombo.*, Playlist.*, PlaylistTrack_position, PlaylistTrack_id
        FROM TrackCombo 
            JOIN PlaylistTrack ON Track_trackId = PlaylistTrack_trackId 
            JOIN Playlist ON PlaylistTrack_playlistId = Playlist_playlistId 
        WHERE PlaylistTrack_id IN (:ids)
        GROUP BY PlaylistTrack_id
        ORDER BY PlaylistTrack_position
        """
    )
    abstract suspend fun listTrackCombosById(vararg ids: UUID): List<PlaylistTrackCombo>

    @Query("SELECT Track.* FROM PlaylistTrack JOIN Track ON Track_trackId = PlaylistTrack_trackId WHERE PlaylistTrack_playlistId = :playlistId")
    abstract suspend fun listTracks(playlistId: UUID): List<Track>

    @Transaction
    open suspend fun moveTrack(playlistId: UUID, from: Int, to: Int) {
        val tracks = _listPlaylistTracks(playlistId)
        val updatedTracks = tracks
            .toMutableList()
            .apply { add(to, removeAt(from)) }
            .mapIndexed { index, track -> track.copy(position = index) }

        if (updatedTracks.isNotEmpty()) _updateTracks(*updatedTracks.filter { !tracks.contains(it) }.toTypedArray())
    }

    @Query("UPDATE Playlist SET Playlist_updated = :updated WHERE Playlist_playlistId = :playlistId")
    abstract suspend fun touchPlaylist(playlistId: UUID, updated: Instant = Instant.now())
}
