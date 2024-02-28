package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpSize
import androidx.media3.common.PlaybackException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.combineEquals
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.combos.AlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.PlaylistTrack
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.PlayerRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
) : DownloadsViewModel(repos), PlayerRepositoryListener {
    private val allArtists = MutableStateFlow<List<Artist>>(emptyList())
    private var deletedPlaylist: Playlist? = null
    private var deletedPlaylistTracks: List<PlaylistTrack> = emptyList()

    val playlists = repos.playlist.playlistsPojos
    val isWelcomeDialogShown = repos.settings.isWelcomeDialogShown
    val umlautify = repos.settings.umlautify

    init {
        repos.player.addListener(this)
        launchOnIOThread {
            repos.artist.flowArtists().collect { artists -> allArtists.value = artists }
        }
    }

    fun addAlbumToLibrary(albumId: UUID) = launchOnIOThread {
        repos.album.addAlbumToLibrary(albumId)
        repos.track.addToLibraryByAlbumId(albumId)
    }

    fun addSelectionToPlaylist(
        selection: Selection,
        playlistId: UUID,
        includeDuplicates: Boolean = true,
        onFinish: (added: Int) -> Unit = {},
    ) = launchOnIOThread {
        onFinish(repos.playlist.addSelectionToPlaylist(selection, playlistId, includeDuplicates))
    }

    fun createPlaylist(playlist: Playlist, selection: Selection? = null) = launchOnIOThread {
        repos.playlist.insertPlaylist(playlist)
        selection?.also { repos.playlist.addSelectionToPlaylist(it, playlist.playlistId) }
    }

    fun deletePlaylist(playlist: Playlist, onFinish: () -> Unit = {}) = launchOnIOThread {
        deletedPlaylist = playlist
        deletedPlaylistTracks = repos.playlist.listPlaylistTracks(playlist.playlistId)
        repos.playlist.deletePlaylist(playlist)
        onFinish()
    }

    fun deleteLocalAlbumFiles(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {

        repos.album.setAlbumsIsLocal(albumIds, false)
        repos.album.listAlbumsWithTracks(albumIds).forEach { combo ->
            deleteLocalAlbumFiles(combo)
        }
        onFinish()
    }

    fun doStartupTasks(context: Context) = launchOnIOThread {
        updateGenreList()
        if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums(context)
        findOrphansAndDuplicates()
        repos.playlist.deleteOrphanPlaylistTracks()
        repos.track.deleteTempTracks()
        repos.album.deleteTempAlbums()
        deleteMarkedAlbums()
        repos.artist.deleteOrphans()
    }

    suspend fun getDuplicatePlaylistTrackCount(playlistId: UUID, selection: Selection) =
        repos.playlist.getDuplicatePlaylistTrackCount(playlistId, selection)

    suspend fun getAlbumCombo(albumId: UUID): AlbumCombo? = repos.album.getAlbumCombo(albumId)

    fun hideAlbums(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {
        repos.album.setAlbumsIsHidden(albumIds, true)
        onFinish()
    }

    fun hideAlbumsAndDeleteFiles(albumIds: Collection<UUID>, onFinish: () -> Unit = {}) = launchOnIOThread {
        repos.album.setAlbumsIsHidden(albumIds, true)
        repos.album.listAlbumsWithTracks(albumIds).forEach { combo ->
            deleteLocalAlbumFiles(combo)
        }
        onFinish()
    }

    suspend fun listSelectionTracks(selection: Selection) = repos.playlist.listSelectionTracks(selection)

    fun setInnerPadding(value: PaddingValues) = repos.settings.setInnerPadding(value)

    fun setLocalMusicUri(value: Uri) = repos.settings.setLocalMusicUri(value)

    fun setContentAreaSize(value: DpSize) = repos.settings.setContentAreaSize(value)

    fun setWelcomeDialogShown(value: Boolean) = repos.settings.setWelcomeDialogShown(value)

    fun undoDeletePlaylist(onFinish: (UUID) -> Unit) = launchOnIOThread {
        deletedPlaylist?.also { playlist ->
            repos.playlist.insertPlaylist(playlist)
            repos.playlist.insertPlaylistTracks(deletedPlaylistTracks)
            deletedPlaylist = null
            deletedPlaylistTracks = emptyList()
            onFinish(playlist.playlistId)
        }
    }

    fun unhideAlbums(albumIds: Collection<UUID>) = launchOnIOThread {
        repos.album.setAlbumsIsHidden(albumIds, false)
    }


    /** PRIVATE METHODS ******************************************************/
    private suspend fun deleteLocalAlbumFiles(combo: AlbumWithTracksCombo) {
        repos.localMedia.deleteAlbumDirectoryAlbumArt(
            albumCombo = combo,
            albumDirectory = repos.settings.getAlbumDirectory(combo),
            tracks = combo.trackCombos.map { it.track },
        )
        repos.track.deleteTrackFiles(combo.trackCombos.map { it.track })
        repos.track.clearLocalUris(combo.trackCombos.map { it.track.trackId })
    }

    private suspend fun deleteMarkedAlbums() {
        val combos = repos.album.listDeletionMarkedAlbumCombos()

        combos.forEach {
            it.album.albumArt?.deleteInternalFiles()
            repos.localMedia.deleteAlbumDirectoryAlbumArt(
                albumCombo = it,
                albumDirectory = repos.settings.getAlbumDirectory(it),
            )
        }
        if (combos.isNotEmpty()) {
            repos.track.deleteTracksByAlbumId(combos.map { it.album.albumId })
            repos.album.deleteAlbums(combos.map { it.album })
        }
    }

    private suspend fun findOrphansAndDuplicates() {
        val allTracks = repos.track.listTracks()
        val allAlbumCombos = repos.album.listAlbumCombos()
        val allAlbumMultimap =
            allAlbumCombos.associateWith { combo -> allTracks.filter { it.albumId == combo.album.albumId } }
        val nonAlbumDuplicateTracks = allTracks
            .combineEquals { a, b -> a.localUri == b.localUri && a.youtubeVideo?.id == b.youtubeVideo?.id }
            .filter { tracks -> tracks.size > 1 }
            .map { tracks -> tracks.filter { it.albumId == null } }
            .flatten()
        // Collect tracks with non-working localUris:
        val brokenUriTracks = repos.localMedia.listTracksWithBrokenLocalUris(allTracks)
        val nonLocalTracks = brokenUriTracks + allTracks.filter { it.localUri == null }
        // Collect albums that have isLocal=true but should have false:
        val noLongerLocalAlbums = allAlbumMultimap
            .filterKeys { it.album.isLocal }
            .filterValues { nonLocalTracks.containsAll(it) }

        // Delete non-album tracks that have duplicates on albums:
        repos.track.deleteTracks(nonAlbumDuplicateTracks)
        // Update tracks with broken localUris:
        repos.track.clearLocalUris(brokenUriTracks.map { it.trackId })
        // Update albums that should have isLocal=true, but don't:
        repos.album.setAlbumsIsLocal(noLongerLocalAlbums.keys.map { it.album.albumId }, false)
    }

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        try {
            val existingGenreNames = repos.album.listTags().map { it.name }.toSet()
            val mbGenreNames = repos.musicBrainz.listAllGenreNames()
            val newTags = mbGenreNames
                .minus(existingGenreNames)
                .map { Tag(name = it, isMusicBrainzGenre = true) }

            if (newTags.isNotEmpty()) repos.album.insertTags(newTags)
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
        launchOnIOThread {
            if (
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                currentCombo != null &&
                (currentCombo.track.youtubeVideo?.metadataRefreshNeeded == true)
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
                        repos.album.getAlbumWithTracks(albumId)?.trackCombos?.forEach {
                            if (it.track.trackId != currentCombo.track.trackId) {
                                ensureTrackMetadata(it.track, forceReload = true)
                            }
                        }
                    }
                    return@launchOnIOThread
                }
            }

            if (lastAction == PlayerRepository.LastAction.PLAY) {
                SnackbarEngine.addError(error.toString())
                withContext(Dispatchers.Main) { repos.player.stop() }
            }
        }
    }
}
