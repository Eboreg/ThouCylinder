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
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.AlbumGenre
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumStyle
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.ArtistPojo
import us.huseli.thoucylinder.dataclasses.Genre
import us.huseli.thoucylinder.dataclasses.Playlist
import us.huseli.thoucylinder.dataclasses.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.Style
import us.huseli.thoucylinder.dataclasses.Track
import java.time.Instant
import java.util.UUID

@Dao
interface MusicDao {
    /** Pseudo-private methods ***********************************************/
    @Query("SELECT EXISTS(SELECT albumId FROM Album WHERE albumId = :albumId)")
    suspend fun _albumExists(albumId: UUID): Boolean

    @Query("DELETE FROM Album")
    suspend fun _clearAlbums()

    @Query("DELETE FROM AlbumGenre WHERE albumId = :albumId")
    suspend fun _clearAlbumGenres(albumId: UUID)

    @Query("DELETE FROM AlbumStyle WHERE albumId = :albumId")
    suspend fun _clearAlbumStyles(albumId: UUID)

    @Query("DELETE FROM PlaylistTrack WHERE playlistId = :playlistId")
    suspend fun _clearPlaylistTracks(playlistId: UUID)

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

    @Insert
    suspend fun _insertPlaylists(vararg playlists: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertPlaylistTracks(vararg playlistTracks: PlaylistTrack)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertStyles(vararg styles: Style)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _insertTracks(vararg tracks: Track)

    @Query("SELECT EXISTS(SELECT playlistId FROM Playlist WHERE playlistId = :playlistId)")
    suspend fun _playlistExists(playlistId: UUID): Boolean

    @Update
    suspend fun _updateAlbums(vararg albums: Album)

    @Update
    suspend fun _updatePlaylists(vararg playlist: Playlist)

    /** Public methods *******************************************************/
    @Transaction
    suspend fun addSelectionToPlaylist(selection: Selection, playlist: AbstractPlaylist) {
        val playlistTracks = mutableListOf<PlaylistTrack>()

        playlistTracks.addAll(
            selection.tracks.mapIndexed { index, track ->
                PlaylistTrack(playlistId = playlist.playlistId, trackId = track.trackId, position = index)
            }
        )
        selection.albums.forEachIndexed { albumIdx, album ->
            getAlbumWithTracks(album.albumId)?.tracks?.also { tracks ->
                playlistTracks.addAll(
                    tracks.mapIndexed { trackIdx, track ->
                        PlaylistTrack(
                            playlistId = playlist.playlistId,
                            trackId = track.trackId,
                            position = albumIdx + trackIdx,
                        )
                    }
                )
            }
        }

        _insertPlaylistTracks(*playlistTracks.toTypedArray())
    }

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

    @Query("SELECT * FROM Album WHERE albumId = :albumId")
    @Transaction
    fun flowAlbumWithTracks(albumId: UUID): Flow<AlbumWithTracksPojo?>

    @Query(
        """
        SELECT a.*, SUM(t.metadatadurationMs) AS durationMs, MIN(t.year) AS minYear, MAX(t.year) AS maxYear,
            COUNT(t.trackId) AS trackCount
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
            COUNT(DISTINCT t.trackId) AS trackCount,
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

    @Query(
        """
        SELECT p.*, COUNT(pt.trackId) AS trackCount, SUM(t.metadatadurationMs) AS totalDurationMs
        FROM Playlist p 
            LEFT JOIN PlaylistTrack pt ON p.playlistId = pt.playlistId 
            LEFT JOIN Track t ON pt.trackId = t.trackId
        GROUP BY p.playlistId
        HAVING p.playlistId IS NOT NULL
        """
    )
    fun flowPlaylists(): Flow<List<PlaylistPojo>>

    @Query("SELECT * FROM Album WHERE albumId = :albumId")
    @Transaction
    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo?

    suspend fun insertTrack(track: Track) {
        _insertTracks(track.copy(isInLibrary = true))
    }

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

    @Query("SELECT t.* FROM Track t JOIN PlaylistTrack pt ON t.trackId = pt.trackId AND pt.playlistId = :playlistId")
    fun pageTracksByPlaylistId(playlistId: UUID): PagingSource<Int, Track>

    @Update
    suspend fun updateTracks(vararg tracks: Track)

    @Transaction
    suspend fun upsertAlbumWithTracks(pojo: AlbumWithTracksPojo) {
        if (_albumExists(pojo.album.albumId)) {
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
                AlbumGenre(albumId = pojo.album.albumId, genreName = it.genreName)
            }.toTypedArray())
        }
        if (pojo.styles.isNotEmpty()) {
            _insertStyles(*pojo.styles.toTypedArray())
            _insertAlbumStyles(*pojo.styles.map {
                AlbumStyle(albumId = pojo.album.albumId, styleName = it.styleName)
            }.toTypedArray())
        }
    }

    @Transaction
    suspend fun upsertPlaylistWithTracks(playlist: Playlist, trackIds: List<UUID>) {
        val now = Instant.now()

        if (_playlistExists(playlist.playlistId)) {
            _updatePlaylists(playlist.copy(updated = now))
            _clearPlaylistTracks(playlist.playlistId)
        } else {
            _insertPlaylists(playlist.copy(created = now, updated = now))
        }
        _insertPlaylistTracks(
            *trackIds.mapIndexed { index, trackId ->
                PlaylistTrack(playlist.playlistId, trackId, index)
            }.toTypedArray()
        )
    }
}
