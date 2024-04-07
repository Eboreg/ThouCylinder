package us.huseli.thoucylinder.repositories

import androidx.room.withTransaction
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.views.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(private val database: Database) {
    private val albumDao = database.albumDao()
    private val playlistDao = database.playlistDao()
    private val trackDao = database.trackDao()

    val playlistsPojos = playlistDao.flowPojos().map { it.toImmutableList() }

    suspend fun addSelectionToPlaylist(
        selection: Selection,
        playlistId: String,
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
            albumDao.setIsInLibrary(true, *unsavedAlbums.map { it.albumId }.toTypedArray())
        }
        if (unsavedTracks.isNotEmpty()) {
            trackDao.setIsInLibrary(unsavedTracks.map { it.trackId }, true)
        }
        if (newPlaylistTracks.isNotEmpty()) playlistDao.insertPlaylistTracks(*newPlaylistTracks.toTypedArray())
        playlistDao.touchPlaylist(playlistId)
        newPlaylistTracks.size
    }

    suspend fun deleteOrphanPlaylistTracks() = playlistDao.deleteOrphanPlaylistTracks()

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    fun flowPlaylist(playlistId: String) = playlistDao.flowPlaylist(playlistId)

    fun flowPlaylistTracks(playlistId: String) = playlistDao.flowTrackCombosByPlaylistId(playlistId)

    suspend fun getDuplicatePlaylistTrackCount(playlistId: String, selection: Selection): Int {
        val selectionTracks = listSelectionTracks(selection)
        val currentTracks = playlistDao.listTracks(playlistId)
        return selectionTracks.intersect(currentTracks.toSet()).size
    }

    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylists(playlist)

    suspend fun insertPlaylistTracks(playlistTracks: Collection<PlaylistTrack>) {
        if (playlistTracks.isNotEmpty()) playlistDao.insertPlaylistTracks(*playlistTracks.toTypedArray())
    }

    suspend fun listPlaylistAlbums(playlistId: String): List<Album> = playlistDao.listAlbums(playlistId)

    suspend fun listPlaylistTrackCombos(playlistId: String): List<PlaylistTrackCombo> =
        playlistDao.listTrackCombosByPlaylistId(playlistId)

    suspend fun listPlaylistTrackCombosById(ids: Collection<String>) =
        if (ids.isNotEmpty()) playlistDao.listTrackCombosByPlaylistTrackId(*ids.toTypedArray()) else emptyList()

    suspend fun listPlaylistTracks(playlistId: String): List<PlaylistTrack> = playlistDao.listPlaylistTracks(playlistId)

    suspend fun listSelectionTracks(selection: Selection): Set<Track> = (
        selection.tracks +
            selection.albums.flatMap { albumDao.listTracks(it.albumId) } +
            selection.albumsWithTracks.flatMap { it.trackCombos.map { trackCombo -> trackCombo.track } }
        ).toSet()

    suspend fun movePlaylistTrack(playlistId: String, from: Int, to: Int) = playlistDao.moveTrack(playlistId, from, to)

    suspend fun removePlaylistTracks(playlistId: String, ids: Collection<String>) = database.withTransaction {
        if (ids.isNotEmpty()) {
            playlistDao.deletePlaylistTracks(*ids.toTypedArray())
            playlistDao.touchPlaylist(playlistId)
        }
    }
}
