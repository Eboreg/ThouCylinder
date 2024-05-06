package us.huseli.thoucylinder.repositories

import androidx.room.withTransaction
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.database.Database
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.views.PlaylistTrackCombo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(private val database: Database) {
    private val playlistDao = database.playlistDao()

    private var deletedPlaylist: Playlist? = null
    private var deletedPlaylistTracks: List<PlaylistTrack> = emptyList()

    val playlistsPojos = playlistDao.flowPojos().map { it.toImmutableList() }

    suspend fun addTracksToPlaylist(
        playlistId: String,
        trackIds: Collection<String>,
        includeDuplicates: Boolean = true,
    ): Int = database.withTransaction {
        val currentTrackIds = playlistDao.listPlaylistTrackIds(playlistId).toSet()
        val toAdd =
            if (!includeDuplicates) trackIds.minus(currentTrackIds)
            else trackIds
        val playlistTracks = toAdd.mapIndexed { index, trackId ->
            PlaylistTrack(
                playlistId = playlistId,
                trackId = trackId,
                position = index + currentTrackIds.size,
            )
        }

        if (playlistTracks.isNotEmpty()) {
            playlistDao.insertPlaylistTracks(*playlistTracks.toTypedArray())
            playlistDao.touchPlaylist(playlistId)
        }

        playlistTracks.size
    }

    suspend fun deleteOrphanPlaylistTracks() = playlistDao.deleteOrphanPlaylistTracks()

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.getPlaylist(playlistId)?.also { playlist ->
            deletedPlaylist = playlist
            deletedPlaylistTracks = playlistDao.listPlaylistTracks(playlistId)
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun flowPlaylist(playlistId: String) = playlistDao.flowPlaylist(playlistId)

    fun flowPlaylistTracks(playlistId: String) = playlistDao.flowTrackCombosByPlaylistId(playlistId)

    suspend fun getDuplicatePlaylistTrackCount(playlistId: String, trackIds: Collection<String>): Int =
        playlistDao.getDuplicateTrackCount(playlistId, trackIds)

    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylists(playlist)

    suspend fun insertPlaylistWithTracks(playlist: Playlist, trackIds: Collection<String>) {
        playlistDao.insertPlaylists(playlist)
        if (trackIds.isNotEmpty()) addTracksToPlaylist(playlist.playlistId, trackIds)
    }

    suspend fun listPlaylistAlbums(playlistId: String): List<Album> = playlistDao.listAlbums(playlistId)

    suspend fun listPlaylistTrackCombos(playlistId: String): List<PlaylistTrackCombo> =
        playlistDao.listTrackCombosByPlaylistId(playlistId)

    suspend fun listPlaylistTrackCombosById(ids: Collection<String>) =
        if (ids.isNotEmpty()) playlistDao.listTrackCombosByPlaylistTrackId(*ids.toTypedArray()) else emptyList()

    suspend fun movePlaylistTrack(playlistId: String, from: Int, to: Int) = playlistDao.moveTrack(playlistId, from, to)

    suspend fun removePlaylistTracks(playlistId: String, ids: Collection<String>) = database.withTransaction {
        if (ids.isNotEmpty()) {
            playlistDao.deletePlaylistTracks(*ids.toTypedArray())
            playlistDao.touchPlaylist(playlistId)
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) = playlistDao.renamePlaylist(playlistId, newName)

    suspend fun undoDeletePlaylist(onFinish: (String) -> Unit) {
        deletedPlaylist?.also { playlist ->
            insertPlaylist(playlist)
            if (deletedPlaylistTracks.isNotEmpty()) {
                playlistDao.insertPlaylistTracks(*deletedPlaylistTracks.toTypedArray())
            }
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(playlist.playlistId)
        }
    }
}
