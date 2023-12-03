package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.PlayerRepositoryListener
import us.huseli.thoucylinder.repositories.Repositories
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
) : DownloadsViewModel(repos), PlayerRepositoryListener {
    private var deletedPlaylist: PlaylistPojo? = null
    private var deletedPlaylistTracks: List<PlaylistTrackPojo> = emptyList()

    val playlists = repos.room.playlists
    val autoImportLocalMusic = repos.settings.autoImportLocalMusic

    init {
        repos.player.addListener(this)
    }

    fun addSelectionToPlaylist(selection: Selection, playlist: PlaylistPojo) =
        viewModelScope.launch {
            repos.room.addSelectionToPlaylist(selection, playlist, playlist.trackCount)
        }

    fun createPlaylist(playlist: Playlist, selection: Selection? = null) = viewModelScope.launch {
        repos.room.insertPlaylist(playlist, selection)
    }

    fun deleteAlbumAndFiles(album: Album) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also { pojo ->
            repos.mediaStore.deleteImages(pojo)
            repos.mediaStore.deleteTracks(pojo.tracks)
            repos.room.deleteAlbumWithTracks(pojo.album)
        }
    }

    fun deletePlaylist(pojo: PlaylistPojo, onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        deletedPlaylist = pojo
        deletedPlaylistTracks = repos.room.listPlaylistTracks(pojo.playlistId)
        repos.room.deletePlaylist(pojo.toPlaylist())
        onFinish?.invoke()
    }

    fun deleteTrackFiles(album: Album) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also { pojo ->
            repos.mediaStore.deleteTracks(pojo.tracks)
            repos.room.updateTracks(pojo.tracks.map { it.copy(mediaStoreData = null) })
            repos.room.updateAlbum(pojo.album.copy(isLocal = false))
        }
    }

    fun doStartupTasks(context: Context) = viewModelScope.launch {
        val existingTracks = repos.room.listTracks()

        if (autoImportLocalMusic.value == true) {
            importNewMediaStoreAlbums(context, existingTracks)
        }
        deleteOrphanTracksAndAlbums(existingTracks)
        repos.mediaStore.deleteOrphanImages(except = repos.room.listImageUris())
        repos.room.deleteTempTracksAndAlbums()
    }

    suspend fun getTrackAlbum(track: Track): Album? = repos.room.getTrackAlbum(track)

    fun removeAlbumFromLibrary(album: Album) = viewModelScope.launch {
        repos.room.deleteAlbumWithTracks(album)
    }

    fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch {
        repos.room.saveAlbumWithTracks(ensureTrackMetadata(pojo, commit = false))
    }

    fun setAutoImportLocalMusic(value: Boolean) = repos.settings.setAutoImportLocalMusic(value)

    fun setMusicImportDirectory(value: String) = repos.settings.setMusicImportDirectory(value)

    fun tagAlbumTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch {
        repos.mediaStore.tagAlbumTracks(ensureTrackMetadata(pojo, commit = true))
    }

    fun undoDeletePlaylist(onFinish: (PlaylistPojo) -> Unit) = viewModelScope.launch {
        deletedPlaylist?.also { pojo ->
            repos.room.insertPlaylist(pojo, deletedPlaylistTracks)
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(pojo)
        }
    }

    fun undoRemoveAlbumFromLibrary(album: Album) = viewModelScope.launch {
        repos.room.undeleteAlbumWithTracks(album)
    }

    /** PRIVATE METHODS ******************************************************/

    private suspend fun deleteOrphanTracksAndAlbums(allTracks: List<Track>) {
        val allAlbums = repos.room.listAlbums()
        val albumMultimap = allAlbums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no existing media files:
        val orphanTracks = repos.mediaStore.listOrphanTracks(allTracks)
        // Separate those that have Youtube connection from those that don't:
        val (realOrphanTracks, youtubeOnlyTracks) = orphanTracks.partition { it.youtubeVideo == null }
        // And albums that _only_ have orphan tracks in them:
        val realOrphanAlbums = albumMultimap
            .filter { (_, tracks) -> realOrphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .keys
        val youtubeOnlyAlbums = albumMultimap
            .filter { (_, tracks) -> youtubeOnlyTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .keys

        // Delete the totally orphaned tracks and albums:
        repos.mediaStore.deleteImagesByTracks(realOrphanTracks)
        repos.mediaStore.deleteImagesByAlbums(realOrphanAlbums)
        repos.room.deleteTracks(realOrphanTracks)
        repos.room.deleteAlbums(realOrphanAlbums)
        // Update the Youtube-only tracks and albums if needed:
        youtubeOnlyAlbums.filter { it.isLocal }.takeIf { it.isNotEmpty() }?.also { albums ->
            repos.room.updateAlbums(albums.map { it.copy(isLocal = false) })
        }
        youtubeOnlyTracks.filter { it.mediaStoreData != null }.takeIf { it.isNotEmpty() }?.also { tracks ->
            repos.room.updateTracks(tracks.map { it.copy(mediaStoreData = null) })
        }
    }

    private suspend fun ensureTrackMetadata(pojo: AlbumWithTracksPojo, commit: Boolean): AlbumWithTracksPojo =
        pojo.copy(tracks = pojo.tracks.map { track -> ensureTrackMetadata(track, commit = commit) })

    /** OVERRIDDEN METHODS ***************************************************/

    override fun onPlayerError(
        error: PlaybackException,
        currentPojo: QueueTrackPojo?,
        lastAction: PlayerRepository.LastAction,
    ) = viewModelScope.launch {
        val metadataIsOld = currentPojo?.track?.youtubeVideo?.expiresAt?.isBefore(Instant.now())

        if (
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
            currentPojo != null &&
            (currentPojo.track.youtubeVideo?.metadata == null || metadataIsOld == true)
        ) {
            val track = ensureTrackMetadata(currentPojo.track, commit = true, forceReload = true)
            val playUri = track.playUri

            if (playUri != null && playUri != currentPojo.uri) {
                repos.player.updateTrack(currentPojo.copy(track = track, uri = playUri))
                if (lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentPojo.position)
            }
        } else if (lastAction == PlayerRepository.LastAction.PLAY) SnackbarEngine.addError(error.toString())
    }
}
