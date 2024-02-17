package us.huseli.thoucylinder.repositories

import androidx.room.withTransaction
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.combos.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(private val database: Database) {
    private val albumDao = database.albumDao()
    private val playlistDao = database.playlistDao()
    private val trackDao = database.trackDao()

    val playlists = playlistDao.flowPlaylists()
    val playlistsPojos = playlistDao.flowPojos()

    suspend fun addSelectionToPlaylist(
        selection: Selection,
        playlistId: UUID,
        includeDuplicates: Boolean = true,
    ): Int = database.withTransaction {
        val selectionTracks = listSelectionTracks(selection)
        val currentTracks = playlistDao.listTracks(playlistId)
        val tracksToAdd =
            if (includeDuplicates) selectionTracks
            else selectionTracks.minus(currentTracks.toSet())
        val newPlaylistTracks = tracksToAdd.mapIndexed { index, track ->
            PlaylistTrack(
                playlistId = playlistId,
                trackId = track.trackId,
                position = index + currentTracks.size,
            )
        }
        val unsavedTracks = tracksToAdd.filter { !it.isInLibrary }
        val unsavedAlbums = (selection.albums + selection.albumsWithTracks.map { it.album }).filter { !it.isInLibrary }

        if (unsavedAlbums.isNotEmpty()) {
            albumDao.setIsInLibrary(unsavedAlbums.map { it.albumId }, true)
        }
        if (unsavedTracks.isNotEmpty()) {
            trackDao.setIsInLibrary(unsavedTracks.map { it.trackId }, true)
        }
        playlistDao.insertPlaylistTracks(*newPlaylistTracks.toTypedArray())
        playlistDao.touchPlaylist(playlistId)
        newPlaylistTracks.size
    }

    suspend fun deleteOrphanPlaylistTracks() = playlistDao.deleteOrphanPlaylistTracks()

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    fun flowPlaylist(playlistId: UUID) = playlistDao.flowPlaylist(playlistId)

    fun flowPlaylistTracks(playlistId: UUID) = playlistDao.flowTrackCombos(playlistId)

    suspend fun getDuplicatePlaylistTrackCount(playlistId: UUID, selection: Selection): Int {
        val selectionTracks = listSelectionTracks(selection)
        val currentTracks = playlistDao.listTracks(playlistId)
        return selectionTracks.intersect(currentTracks.toSet()).size
    }

    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylists(playlist)

    suspend fun insertPlaylistTracks(playlistTracks: Collection<PlaylistTrack>) {
        if (playlistTracks.isNotEmpty()) playlistDao.insertPlaylistTracks(*playlistTracks.toTypedArray())
    }

    suspend fun listPlaylistAlbums(playlistId: UUID): List<Album> = playlistDao.listAlbums(playlistId)

    suspend fun listPlaylistTrackCombos(playlistId: UUID): List<PlaylistTrackCombo> =
        playlistDao.listTrackCombos(playlistId)

    suspend fun listPlaylistTracks(playlistId: UUID): List<PlaylistTrack> = playlistDao.listPlaylistTracks(playlistId)

    suspend fun listSelectionTracks(selection: Selection): Set<Track> {
        return (
            selection.tracks +
                selection.albums.flatMap { albumDao.listTracks(it.albumId) } +
                selection.albumsWithTracks.flatMap { it.tracks }
            ).toSet()
    }

    suspend fun movePlaylistTrack(playlistId: UUID, from: Int, to: Int) = playlistDao.moveTrack(playlistId, from, to)

    suspend fun removePlaylistTracks(tracks: List<PlaylistTrack>) = database.withTransaction {
        playlistDao.deletePlaylistTracks(*tracks.toTypedArray())
        tracks.map { it.playlistId }.toSet().forEach { playlistId -> playlistDao.touchPlaylist(playlistId) }
    }
}
