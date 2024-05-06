package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.content.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : DownloadsViewModel(repos, managers) {
    private val _isCoverExpanded = MutableStateFlow(false)

    val currentTrackExists = repos.player.currentCombo.map { it != null }.distinctUntilChanged().stateLazily(false)
    val isCoverExpanded = _isCoverExpanded.asStateFlow()
    val isWelcomeDialogShown: StateFlow<Boolean> = repos.settings.isWelcomeDialogShown
    val playlistPojos: StateFlow<ImmutableList<PlaylistPojo>> =
        repos.playlist.playlistsPojos.stateLazily(persistentListOf())
    val umlautify: StateFlow<Boolean> = repos.settings.umlautify

    suspend fun addTracksToPlaylist(
        playlistId: String,
        trackIds: Collection<String>,
        includeDuplicates: Boolean = true,
    ): Int = withContext(Dispatchers.IO) {
        repos.track.addToLibrary(trackIds)
        repos.playlist.addTracksToPlaylist(playlistId, trackIds, includeDuplicates)
    }

    fun collapseCover() {
        _isCoverExpanded.value = false
    }

    fun createPlaylist(playlist: Playlist, addTracks: Collection<String>) {
        launchOnIOThread {
            repos.track.addToLibrary(addTracks)
            repos.playlist.insertPlaylistWithTracks(playlist, addTracks)
        }
    }

    fun deletePlaylist(playlistId: String, context: Context, onRestored: (String) -> Unit = {}) {
        launchOnIOThread {
            repos.playlist.deletePlaylist(playlistId)

            SnackbarEngine.addInfo(
                message = context.getString(R.string.the_playlist_was_removed).umlautify(),
                actionLabel = context.getString(R.string.undo).umlautify(),
                onActionPerformed = {
                    launchOnIOThread {
                        repos.playlist.undoDeletePlaylist { playlistId ->
                            SnackbarEngine.addInfo(
                                message = context.getString(R.string.the_playlist_was_restored).umlautify(),
                                actionLabel = context.getString(R.string.go_to_playlist).umlautify(),
                                onActionPerformed = { onRestored(playlistId) },
                            )
                        }
                    }
                },
            )
        }
    }

    fun enqueueTrack(trackId: String) = managers.player.enqueueTracks(listOf(trackId))

    fun expandCover() {
        _isCoverExpanded.value = true
    }

    fun getAlbumIdByTrackId(trackId: String, callback: (String) -> Unit) {
        launchOnIOThread {
            repos.track.getAlbumIdByTrackId(trackId)?.also { withContext(Dispatchers.Main) { callback(it) } }
        }
    }

    suspend fun getDuplicatePlaylistTrackCount(playlistId: String, trackIds: Collection<String>) =
        withContext(Dispatchers.IO) { repos.playlist.getDuplicatePlaylistTrackCount(playlistId, trackIds) }

    fun handleLastFmIntent(intent: Intent, onError: (Exception) -> Unit) {
        launchOnIOThread {
            intent.data?.getQueryParameter("token")?.also { authToken ->
                try {
                    repos.lastFm.fetchSession(authToken)
                } catch (e: Exception) {
                    repos.lastFm.setScrobble(false)
                    logError("handleIntent: $e", e)
                    onError(e)
                }
            }
        }
    }

    fun handleSpotifyIntent(intent: Intent, onError: (Exception) -> Unit) {
        launchOnIOThread {
            try {
                repos.spotify.oauth2PKCE.handleIntent(intent)
            } catch (e: Exception) {
                logError("handleIntent: $e", e)
                onError(e)
            }
        }
    }

    fun playTrack(trackId: String) = managers.player.playTracks(listOf(trackId))

    fun setWelcomeDialogShown(value: Boolean) = repos.settings.setWelcomeDialogShown(value)

    fun startAlbumRadio(albumId: String) = managers.radio.startAlbumRadio(albumId)

    fun startArtistRadio(artistId: String) = managers.radio.startArtistRadio(artistId)

    fun startTrackRadio(trackId: String) = managers.radio.startTrackRadio(trackId)
}
