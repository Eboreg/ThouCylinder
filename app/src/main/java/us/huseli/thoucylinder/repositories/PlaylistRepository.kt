package us.huseli.thoucylinder.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.PlaylistTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.toPlaylistTracks
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(private val database: Database) {
    private val albumDao = database.albumDao()
    private val playlistDao = database.playlistDao()
    private val trackDao = database.trackDao()

    val playlists = playlistDao.flowPojos()

    suspend fun addSelectionToPlaylist(
        selection: Selection,
        playlist: AbstractPlaylist,
        offset: Int,
    ) = database.withTransaction {
        val tracks = getTracksFromSelection(selection)
        val playlistTracks = tracks.mapIndexed { index, track ->
            PlaylistTrack(
                playlistId = playlist.playlistId,
                trackId = track.trackId,
                position = index + offset,
            )
        }
        val unsavedTracks = tracks.filter { !it.isInLibrary }
        val unsavedAlbums = (selection.albums + selection.albumsWithTracks.map { it.album }).filter { !it.isInLibrary }

        if (unsavedAlbums.isNotEmpty()) {
            albumDao.setIsInLibrary(unsavedAlbums.map { it.albumId }, true)
        }
        if (unsavedTracks.isNotEmpty()) {
            trackDao.setIsInLibrary(unsavedTracks.map { it.trackId }, true)
        }
        playlistDao.insertTracks(*playlistTracks.toTypedArray())
        playlistDao.touchPlaylist(playlist.playlistId)
    }

    suspend fun deleteOrphanPlaylistTracks() = playlistDao.deleteOrphanPlaylistTracks()

    suspend fun deletePlaylist(playlist: AbstractPlaylist) = playlistDao.deletePlaylist(playlist.toPlaylist())

    suspend fun insertPlaylist(playlist: AbstractPlaylist) = playlistDao.insertPlaylists(playlist.toPlaylist())

    suspend fun insertPlaylistTracks(playlistTracks: Collection<PlaylistTrack>) {
        if (playlistTracks.isNotEmpty()) playlistDao.insertTracks(*playlistTracks.toTypedArray())
    }

    suspend fun listPlaylistAlbums(playlistId: UUID): List<Album> = playlistDao.listAlbums(playlistId)

    suspend fun listPlaylistTracks(playlistId: UUID): List<PlaylistTrackCombo> = playlistDao.listTracks(playlistId)

    fun pageTrackCombosByPlaylistId(playlistId: UUID): Pager<Int, PlaylistTrackCombo> =
        Pager(config = PagingConfig(pageSize = 100)) { playlistDao.pageTracks(playlistId) }

    suspend fun removePlaylistTracks(combos: List<PlaylistTrackCombo>) = database.withTransaction {
        playlistDao.deletePlaylistTracks(*combos.toPlaylistTracks().toTypedArray())
        combos.map { it.playlist.playlistId }.toSet().forEach { playlistId -> playlistDao.touchPlaylist(playlistId) }
    }

    private suspend fun getTracksFromSelection(selection: Selection): Set<Track> {
        return (
            selection.tracks +
                selection.queueTracks.map { it.track } +
                selection.albums.flatMap { albumDao.listTracks(it.albumId) } +
                selection.albumsWithTracks.flatMap { it.tracks }
            ).toSet()
    }
}
