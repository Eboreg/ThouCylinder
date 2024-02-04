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
import us.huseli.retaintheme.extensions.capitalized
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.Constants.MUSICBRAINZ_GENRES_URL
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractPlaylist
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.getString
import us.huseli.thoucylinder.repositories.PlayerRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
) : DownloadsViewModel(repos), PlayerRepositoryListener {
    private var deletedPlaylist: AbstractPlaylist? = null
    private var deletedPlaylistTracks: List<PlaylistTrackPojo> = emptyList()

    val playlists = repos.playlist.playlists
    val isWelcomeDialogShown = repos.settings.isWelcomeDialogShown

    init {
        repos.player.addListener(this)
    }

    fun addAlbumToLibrary(albumId: UUID) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.addToLibrary(albumId)
        repos.track.addToLibraryByAlbumId(albumId)
    }

    fun addSelectionToPlaylist(selection: Selection, playlistPojo: PlaylistPojo) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.playlist.addSelectionToPlaylist(selection, playlistPojo, playlistPojo.trackCount)
        }

    fun createPlaylist(playlist: AbstractPlaylist, selection: Selection? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.playlist.insertPlaylist(playlist.toPlaylist())
            selection?.also { repos.playlist.addSelectionToPlaylist(it, playlist, 0) }
        }

    fun deletePlaylist(pojo: AbstractPlaylist, onFinish: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        deletedPlaylist = pojo
        deletedPlaylistTracks = repos.playlist.listPlaylistTracks(pojo.playlistId)
        repos.playlist.deletePlaylist(pojo)
        onFinish()
    }

    fun deleteLocalAlbumFiles(albumId: UUID, onFinish: () -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.setAlbumIsLocal(albumId, false)
        repos.album.getAlbumWithTracks(albumId)?.also { pojo ->
            deleteLocalAlbumFiles(pojo)
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

    suspend fun getTrackAlbum(albumId: UUID?): Album? = albumId?.let { repos.album.getAlbum(it) }

    fun markAlbumForDeletion(albumId: UUID, onFinish: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.setAlbumIsDeleted(albumId, true)
        repos.album.setAlbumIsLocal(albumId, false)
        repos.album.getAlbumWithTracks(albumId)?.also { pojo ->
            deleteLocalAlbumFiles(pojo)
            onFinish()
        }
    }

    fun saveTrack(track: Track) = viewModelScope.launch {
        repos.track.updateTrack(track)
    }

    fun setAlbumIsHidden(albumId: UUID, value: Boolean, onFinish: () -> Unit = {}) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.album.setAlbumIsHidden(albumId, value)
            onFinish()
        }

    fun setInnerPadding(value: PaddingValues) = repos.settings.setInnerPadding(value)

    fun setLocalMusicUri(value: Uri) = repos.settings.setLocalMusicUri(value)

    fun setContentAreaSize(value: DpSize) = repos.settings.setContentAreaSize(value)

    fun setWelcomeDialogShown(value: Boolean) = repos.settings.setWelcomeDialogShown(value)

    fun undoDeletePlaylist(onFinish: (AbstractPlaylist) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        deletedPlaylist?.also { pojo ->
            repos.playlist.insertPlaylist(pojo)
            repos.playlist.insertPlaylistTracks(deletedPlaylistTracks.map { it.toPlaylistTrack() })
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(pojo)
        }
    }

    fun unmarkAlbumForDeletion(albumId: UUID) = viewModelScope.launch {
        repos.album.setAlbumIsDeleted(albumId, false)
    }

    /** PRIVATE METHODS ******************************************************/
    private suspend fun deleteLocalAlbumFiles(pojo: AlbumWithTracksPojo) {
        withContext(Dispatchers.IO) {
            repos.localMedia.deleteAlbumDirectoryAlbumArt(pojo)
            repos.track.deleteTrackFiles(pojo.tracks)
        }
        repos.track.updateTracks(pojo.tracks.map { it.copy(localUri = null) })
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
        val realOrphanAlbumPojos = albumMultimap
            .filter { (_, tracks) -> realOrphanTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .map { (album, tracks) -> AlbumWithTracksPojo(album = album, tracks = tracks) }
        val youtubeOnlyAlbums = albumMultimap
            .filter { (_, tracks) -> youtubeOnlyTracks.map { it.trackId }.containsAll(tracks.map { it.trackId }) }
            .keys

        // Delete the totally orphaned tracks and albums:
        realOrphanAlbumPojos.forEach {
            it.album.albumArt?.deleteInternalFiles()
            repos.localMedia.deleteAlbumDirectoryAlbumArt(it)
        }
        repos.track.deleteTracks(realOrphanTracks)
        repos.album.deleteAlbumPojos(realOrphanAlbumPojos)
        // Update the Youtube-only tracks and albums if needed:
        youtubeOnlyAlbums.filter { it.isLocal }.takeIf { it.isNotEmpty() }?.also { albums ->
            repos.album.updateAlbums(albums.map { it.copy(isLocal = false) })
        }
        youtubeOnlyTracks.filter { it.localUri != null }.takeIf { it.isNotEmpty() }?.also { tracks ->
            repos.track.updateTracks(tracks.map { it.copy(localUri = null) })
        }
    }

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        try {
            val existingGenreNames = repos.album.listGenres().map { it.genreName }.toSet()
            val mbGenreNames = Request.get(MUSICBRAINZ_GENRES_URL)
                .connect()
                .getString()
                .split('\n')
                .map { it.capitalized() }
                .toSet()
            val newGenres = mbGenreNames.minus(existingGenreNames).map { Genre(it) }

            repos.album.insertGenres(newGenres)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "updateGenreList: $e", e)
        }
    }


    /** OVERRIDDEN METHODS ***************************************************/
    override fun onPlayerError(
        error: PlaybackException,
        currentPojo: QueueTrackPojo?,
        lastAction: PlayerRepository.LastAction,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadataIsOld = currentPojo?.track?.youtubeVideo?.expiresAt?.isBefore(Instant.now())

            if (
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                currentPojo != null &&
                (currentPojo.track.youtubeVideo?.metadata == null || metadataIsOld == true)
            ) {
                val track = ensureTrackMetadata(currentPojo.track)
                val playUri = track.playUri

                if (playUri != null && playUri != currentPojo.uri) {
                    withContext(Dispatchers.Main) {
                        repos.player.updateTrack(currentPojo.copy(track = track, uri = playUri))
                        if (lastAction == PlayerRepository.LastAction.PLAY) repos.player.play(currentPojo.position)
                    }
                }
            } else if (lastAction == PlayerRepository.LastAction.PLAY) SnackbarEngine.addError(error.toString())
        }
    }
}
