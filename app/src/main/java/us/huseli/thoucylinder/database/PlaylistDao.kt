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
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.views.PlaylistTrackCombo
import java.time.Instant

@Dao
abstract class PlaylistDao {
    @Query("SELECT * FROM PlaylistTrack WHERE PlaylistTrack_playlistId = :playlistId ORDER BY PlaylistTrack_position")
    protected abstract suspend fun _listPlaylistTracks(playlistId: String): List<PlaylistTrack>

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
    abstract suspend fun deletePlaylistTracks(vararg ids: String)

    @Query(
        """
        SELECT Playlist_playlistId, Playlist_name, COUNT(PlaylistTrack_trackId) AS trackCount,
            SUM(Track_metadata_durationMs) AS totalDurationMs
        FROM Playlist 
            LEFT JOIN PlaylistTrack ON Playlist_playlistId = PlaylistTrack_playlistId 
            LEFT JOIN Track ON PlaylistTrack_trackId = Track_trackId
        GROUP BY Playlist_playlistId
        HAVING Playlist_playlistId IS NOT NULL
        """
    )
    abstract fun flowPojos(): Flow<List<PlaylistPojo>>

    @Query("SELECT * FROM Playlist WHERE Playlist_playlistId = :playlistId")
    abstract fun flowPlaylist(playlistId: String): Flow<Playlist?>

    @Transaction
    @Query("SELECT * FROM PlaylistTrackCombo WHERE Playlist_playlistId = :playlistId")
    abstract fun flowTrackCombosByPlaylistId(playlistId: String): Flow<List<PlaylistTrackCombo>>

    @Query("SELECT * FROM Playlist WHERE Playlist_playlistId = :playlistId")
    abstract suspend fun getPlaylist(playlistId: String): Playlist?

    @Query("SELECT COUNT(*) FROM PlaylistTrack WHERE PlaylistTrack_trackId IN (:trackIds) AND PlaylistTrack_playlistId = :playlistId")
    abstract suspend fun getDuplicateTrackCount(playlistId: String, trackIds: Collection<String>): Int

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
    abstract suspend fun listAlbums(playlistId: String): List<Album>

    @Query("SELECT PlaylistTrack_trackId FROM PlaylistTrack WHERE PlaylistTrack_playlistId = :playlistId")
    abstract suspend fun listPlaylistTrackIds(playlistId: String): List<String>

    @Query("SELECT * FROM PlaylistTrack WHERE PlaylistTrack_playlistId = :playlistId")
    abstract suspend fun listPlaylistTracks(playlistId: String): List<PlaylistTrack>

    @Transaction
    @Query("SELECT * FROM PlaylistTrackCombo WHERE Playlist_playlistId = :playlistId")
    abstract suspend fun listTrackCombosByPlaylistId(playlistId: String): List<PlaylistTrackCombo>

    @Transaction
    @Query("SELECT * FROM PlaylistTrackCombo WHERE PlaylistTrack_id IN (:ids)")
    abstract suspend fun listTrackCombosByPlaylistTrackId(vararg ids: String): List<PlaylistTrackCombo>

    @Transaction
    open suspend fun moveTrack(playlistId: String, from: Int, to: Int) {
        val tracks = _listPlaylistTracks(playlistId)
        val updatedTracks = tracks
            .toMutableList()
            .apply { add(to, removeAt(from)) }
            .mapIndexed { index, track -> track.copy(position = index) }

        if (updatedTracks.isNotEmpty()) {
            _updateTracks(*updatedTracks.filter { !tracks.contains(it) }.toTypedArray())
            touchPlaylist(playlistId)
        }
    }

    @Query("UPDATE Playlist SET Playlist_name = :newName, Playlist_updated = :updated WHERE Playlist_playlistId = :playlistId")
    abstract suspend fun renamePlaylist(playlistId: String, newName: String, updated: String = Instant.now().toString())

    @Query("UPDATE Playlist SET Playlist_updated = :updated WHERE Playlist_playlistId = :playlistId")
    abstract suspend fun touchPlaylist(playlistId: String, updated: String = Instant.now().toString())
}
