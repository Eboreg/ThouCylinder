package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.Constants.MUSICBRAINZ_GENRES_URL
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.capitalized
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
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
) : DownloadsViewModel(repos), PlayerRepositoryListener {
    private var deletedPlaylist: AbstractPlaylist? = null
    private var deletedPlaylistTracks: List<PlaylistTrackPojo> = emptyList()

    val playlists = repos.playlist.playlists

    init {
        repos.player.addListener(this)
    }

    fun addSelectionToPlaylist(selection: Selection, playlistPojo: PlaylistPojo) =
        viewModelScope.launch {
            repos.playlist.addSelectionToPlaylist(selection, playlistPojo, playlistPojo.trackCount)
        }

    fun createPlaylist(playlist: AbstractPlaylist, selection: Selection? = null) = viewModelScope.launch {
        repos.playlist.insertPlaylist(playlist.toPlaylist())
        selection?.also { repos.playlist.addSelectionToPlaylist(it, playlist, 0) }
    }

    fun deleteAlbumAndFiles(album: Album, onFinish: () -> Unit = {}) = viewModelScope.launch {
        repos.album.getAlbumWithTracks(album.albumId)?.also { pojo ->
            withContext(Dispatchers.IO) {
                repos.album.deleteAlbumArt(album)
                repos.track.deleteTrackFiles(pojo.tracks)
            }
            repos.track.deleteTracks(pojo.tracks)
            repos.album.deleteAlbum(album)
            onFinish()
        }
    }

    fun deletePlaylist(pojo: AbstractPlaylist, onFinish: () -> Unit = {}) = viewModelScope.launch {
        deletedPlaylist = pojo
        deletedPlaylistTracks = repos.playlist.listPlaylistTracks(pojo.playlistId)
        repos.playlist.deletePlaylist(pojo)
        onFinish()
    }

    fun deleteLocalFiles(album: Album, onFinish: () -> Unit = {}) = viewModelScope.launch {
        repos.album.getAlbumWithTracks(album.albumId)?.also { pojo ->
            withContext(Dispatchers.IO) {
                repos.album.deleteAlbumLocalImageFiles(pojo)
                repos.track.deleteTrackFiles(pojo.tracks)
            }
            repos.track.updateTracks(pojo.tracks.map { it.copy(localUri = null) })
            repos.album.updateAlbum(pojo.album.copy(isLocal = false))
            onFinish()
        }
    }

    fun doStartupTasks(context: Context) = viewModelScope.launch {
        val existingTracks = repos.track.listTracks()

        if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums(context, existingTracks)
        deleteOrphanTracksAndAlbums(existingTracks, context)
        repos.playlist.deleteOrphanPlaylistTracks()
        repos.track.deleteTempTracks()
        repos.album.deleteTempAlbums()
        repos.album.deleteMarkedAlbums()
        updateGenreList()
    }

    suspend fun getTrackAlbum(track: Track): Album? = track.albumId?.let { repos.album.getAlbum(it) }

    fun markAlbumForDeletion(album: Album) = viewModelScope.launch {
        repos.album.updateAlbum(album.copy(isDeleted = true))
    }

    fun setMusicDownloadUri(value: Uri) = repos.settings.setMusicDownloadUri(value)

    fun tagAlbumTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch {
        repos.localMedia.tagAlbumTracks(ensureTrackMetadata(pojo))
    }

    fun undoDeletePlaylist(onFinish: (AbstractPlaylist) -> Unit) = viewModelScope.launch {
        deletedPlaylist?.also { pojo ->
            repos.playlist.insertPlaylist(pojo)
            repos.playlist.insertPlaylistTracks(deletedPlaylistTracks.map { it.toPlaylistTrack() })
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(pojo)
        }
    }

    fun unmarkAlbumForDeletion(album: Album) = viewModelScope.launch {
        repos.album.updateAlbum(album.copy(isDeleted = false))
    }

    /** PRIVATE METHODS ******************************************************/
    private suspend fun deleteOrphanTracksAndAlbums(allTracks: List<Track>, context: Context) {
        val allAlbums = repos.album.listAlbums()
        val albumMultimap = allAlbums.associateWith { album -> allTracks.filter { it.albumId == album.albumId } }
        // Collect tracks that have no existing media files:
        val orphanTracks = repos.localMedia.listOrphanTracks(allTracks)
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
        realOrphanAlbums.forEach { it.albumArt?.delete(context) }
        repos.track.deleteTracks(realOrphanTracks)
        repos.album.deleteAlbums(realOrphanAlbums)
        // Update the Youtube-only tracks and albums if needed:
        youtubeOnlyAlbums.filter { it.isLocal }.takeIf { it.isNotEmpty() }?.also { albums ->
            repos.album.updateAlbums(albums.map { it.copy(isLocal = false) })
        }
        youtubeOnlyTracks.filter { it.localUri != null }.takeIf { it.isNotEmpty() }?.also { tracks ->
            repos.track.updateTracks(tracks.map { it.copy(localUri = null) })
        }
    }

    private suspend fun ensureTrackMetadata(pojo: AlbumWithTracksPojo): AlbumWithTracksPojo =
        pojo.copy(tracks = pojo.tracks.map { track -> ensureTrackMetadata(track, commit = true) })

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        val existingGenreNames = repos.album.listGenres().map { it.genreName }.toSet()
        val mbGenreNames = Request.get(MUSICBRAINZ_GENRES_URL)
            .connect()
            .getString()
            .split('\n')
            .map { it.capitalized() }
            .toSet()
        val newGenres = mbGenreNames.minus(existingGenreNames).map { Genre(it) }

        repos.album.insertGenres(newGenres)
    }


    /** OVERRIDDEN METHODS ***************************************************/
    override fun onPlayerError(
        error: PlaybackException,
        currentPojo: QueueTrackPojo?,
        lastAction: PlayerRepository.LastAction,
    ) {
        viewModelScope.launch {
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
}
