package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpSize
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.PlayerRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
) : DownloadsViewModel(repos), PlayerRepositoryListener {
    private var deletedPlaylist: Playlist? = null
    private var deletedPlaylistTracks: List<PlaylistTrack> = emptyList()

    val playlists = repos.playlist.playlistsPojos
    val isWelcomeDialogShown = repos.settings.isWelcomeDialogShown
    val umlautify = repos.settings.umlautify

    init {
        repos.player.addListener(this)
    }

    fun addAlbumToLibrary(albumId: UUID) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.addToLibrary(albumId)
        repos.track.addToLibraryByAlbumId(albumId)
    }

    fun addSelectionToPlaylist(
        selection: Selection,
        playlistId: UUID,
        includeDuplicates: Boolean = true,
        onFinish: (added: Int) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        onFinish(repos.playlist.addSelectionToPlaylist(selection, playlistId, includeDuplicates))
    }

    fun createPlaylist(playlist: Playlist, selection: Selection? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.playlist.insertPlaylist(playlist)
            selection?.also { repos.playlist.addSelectionToPlaylist(it, playlist.playlistId) }
        }

    fun deletePlaylist(playlist: Playlist, onFinish: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        deletedPlaylist = playlist
        deletedPlaylistTracks = repos.playlist.listPlaylistTracks(playlist.playlistId)
        repos.playlist.deletePlaylist(playlist)
        onFinish()
    }

    fun deleteLocalAlbumFiles(albumId: UUID, onFinish: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.setAlbumIsLocal(albumId, false)
        repos.album.getAlbumWithTracks(albumId)?.also { combo ->
            deleteLocalAlbumFiles(combo)
            onFinish()
        }
    }

    fun doStartupTasks(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val existingTracks = repos.track.listTracks()

        if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums(context, existingTracks)
        deleteOrphanTracksAndAlbums(existingTracks)
        repos.playlist.deleteOrphanPlaylistTracks()
        repos.track.deleteTempTracks()
        repos.album.deleteTempAlbums()
        deleteMarkedAlbums()
        updateGenreList()
    }

    suspend fun getDuplicatePlaylistTrackCount(playlistId: UUID, selection: Selection) =
        repos.playlist.getDuplicatePlaylistTrackCount(playlistId, selection)

    suspend fun getTrackAlbum(albumId: UUID?): Album? = albumId?.let { repos.album.getAlbum(it) }

    suspend fun listSelectionTracks(selection: Selection) = repos.playlist.listSelectionTracks(selection)

    fun markAlbumForDeletion(albumId: UUID, onFinish: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.setAlbumIsDeleted(albumId, true)
        repos.album.setAlbumIsLocal(albumId, false)
        repos.album.getAlbumWithTracks(albumId)?.also { combo ->
            deleteLocalAlbumFiles(combo)
            onFinish()
        }
    }

    fun setAlbumIsHidden(albumId: UUID, value: Boolean, onFinish: () -> Unit = {}) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.album.setAlbumIsHidden(albumId, value)
            onFinish()
        }

    fun setAlbumIsInLibrary(albumId: UUID, value: Boolean, onFinish: () -> Unit = {}) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.album.setAlbumIsInLibrary(albumId, value)
            onFinish()
        }

    fun setInnerPadding(value: PaddingValues) = repos.settings.setInnerPadding(value)

    fun setLocalMusicUri(value: Uri) = repos.settings.setLocalMusicUri(value)

    fun setContentAreaSize(value: DpSize) = repos.settings.setContentAreaSize(value)

    fun setWelcomeDialogShown(value: Boolean) = repos.settings.setWelcomeDialogShown(value)

    fun undoDeletePlaylist(onFinish: (UUID) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        deletedPlaylist?.also { playlist ->
            repos.playlist.insertPlaylist(playlist)
            repos.playlist.insertPlaylistTracks(deletedPlaylistTracks)
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(playlist.playlistId)
        }
    }

    fun unmarkAlbumForDeletion(albumId: UUID) = viewModelScope.launch {
        repos.album.setAlbumIsDeleted(albumId, false)
    }

    /** PRIVATE METHODS ******************************************************/
    private suspend fun deleteLocalAlbumFiles(combo: AlbumWithTracksCombo) {
        repos.localMedia.deleteAlbumDirectoryAlbumArt(combo)
        repos.track.deleteTrackFiles(combo.tracks)
        repos.track.clearLocalUris(combo.tracks.map { it.trackId })
    }

    private suspend fun deleteMarkedAlbums() {
        val albums = repos.album.listDeletionMarkedAlbums()

        albums.forEach {
            it.albumArt?.deleteInternalFiles()
            repos.localMedia.deleteAlbumDirectoryAlbumArt(it)
        }
        repos.track.deleteTracksByAlbumId(albums.map { it.albumId })
        repos.album.deleteAlbums(albums)
    }

    private suspend fun deleteOrphanTracksAndAlbums(allTracks: List<Track>) {
        val allAlbums = repos.album.listAlbums()
        val albumMultimap = allAlbums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no existing media files:
        val orphanTracks = repos.localMedia.listOrphanTracks(allTracks)
        // Separate those that have Youtube connection from those that don't:
        val (realOrphanTracks, youtubeOnlyTracks) = orphanTracks.partition { it.youtubeVideo == null }
        // And albums that _only_ have orphan tracks in them:
        val realOrphanAlbumCombos = albumMultimap
            .filter { (_, tracks) -> realOrphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .map { (album, tracks) -> AlbumWithTracksCombo(album = album, tracks = tracks) }
        val youtubeOnlyAlbums = albumMultimap
            .filter { (_, tracks) -> youtubeOnlyTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .keys

        // Delete the totally orphaned tracks and albums:
        realOrphanAlbumCombos.forEach {
            it.album.albumArt?.deleteInternalFiles()
            repos.localMedia.deleteAlbumDirectoryAlbumArt(it)
        }
        repos.track.deleteTracks(realOrphanTracks)
        repos.album.deleteAlbums(realOrphanAlbumCombos.map { it.album })
        // Update the Youtube-only tracks and albums if needed:
        youtubeOnlyAlbums.filter { it.isLocal }.takeIf { it.isNotEmpty() }?.also { albums ->
            repos.album.setAlbumsIsLocal(albums.map { it.albumId }, false)
        }
        youtubeOnlyTracks.filter { it.localUri != null }.takeIf { it.isNotEmpty() }?.also { tracks ->
            repos.track.clearLocalUris(tracks.map { it.trackId })
        }
    }

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        try {
            val existingGenreNames = repos.album.listTags().map { it.name }.toSet()
            val mbGenreNames = repos.musicBrainz.listAllGenres()
            val newTags = mbGenreNames
                .minus(existingGenreNames)
                .map { Tag(name = it, isMusicBrainzGenre = true) }

            repos.album.insertTags(newTags)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "updateGenreList: $e", e)
        }
    }


    /** OVERRIDDEN METHODS ***************************************************/
    override fun onPlayerError(
        error: PlaybackException,
        currentCombo: QueueTrackCombo?,
        lastAction: PlayerRepository.LastAction,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadataIsOld = currentCombo?.track?.youtubeVideo?.expiresAt?.isBefore(Instant.now())

            if (
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                currentCombo != null &&
                (currentCombo.track.youtubeVideo?.metadata == null || metadataIsOld == true)
            ) {
                val track = ensureTrackMetadata(currentCombo.track, forceReload = true)
                val playUri = track.playUri

                if (playUri != null && playUri != currentCombo.uri) {
                    withContext(Dispatchers.Main) {
                        repos.player.updateTrack(currentCombo.copy(track = track, uri = playUri))
                        if (lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentCombo.position)
                    }
                    // The rest of the album probably has outdated URLs, too:
                    currentCombo.album?.albumId?.also { albumId ->
                        repos.album.getAlbumWithTracks(albumId)?.tracks?.forEach {
                            if (it.trackId != currentCombo.track.trackId) ensureTrackMetadata(it)
                        }
                    }
                    return@launch
                }
            }

            if (lastAction == PlayerRepository.LastAction.PLAY) {
                SnackbarEngine.addError(error.toString())
                repos.player.stop()
            }
        }
    }
}
