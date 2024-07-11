package us.huseli.thoucylinder.viewmodels

import android.content.Intent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.Umlautify
import us.huseli.thoucylinder.managers.ExternalContentManager
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : DownloadsViewModel(repos, managers) {
    val currentTrackExists = repos.player.currentCombo.map { it != null }.distinctUntilChanged().stateLazily(false)
    val isWelcomeDialogShown: StateFlow<Boolean> = repos.settings.isWelcomeDialogShown
    val contentSize: StateFlow<Size> = repos.settings.contentSize
    val umlautifier = Umlautify.umlautifier.stateLazily(Umlautify.disabledUmlautifier)
    val albumImportProgress = managers.external.albumImportProgress
        .map { if (it.isActive) it.progress else null }
        .stateLazily()

    fun addAlbumToLibrary(
        albumId: String,
        onGotoAlbumClick: (String) -> Unit,
        onGotoLibraryClick: (() -> Unit)? = null,
    ) {
        launchOnIOThread {
            managers.library.addAlbumsToLibrary(
                albumIds = listOf(albumId),
                onGotoAlbumClick = onGotoAlbumClick,
                onGotoLibraryClick = onGotoLibraryClick,
            )
        }
    }

    fun enqueueAlbum(albumId: String) = managers.player.enqueueAlbums(listOf(albumId))

    fun enqueueArtist(artistId: String) = managers.player.enqueueArtist(artistId)

    fun enqueueTrack(trackId: String) = managers.player.enqueueTracks(listOf(trackId))

    fun handleLastFmIntent(intent: Intent) {
        launchOnIOThread {
            intent.data?.getQueryParameter("token")?.also { authToken ->
                try {
                    repos.lastFm.getSession(authToken)
                } catch (e: Exception) {
                    repos.lastFm.setScrobble(false)
                    repos.message.onLastFmAuthError(error = e)
                    logError("handleIntent: $e", e)
                }
            }
        }
    }

    fun handleSpotifyIntent(intent: Intent) {
        launchOnIOThread {
            try {
                repos.spotify.oauth2PKCE.handleIntent(intent)
            } catch (e: Exception) {
                logError("handleIntent: $e", e)
                repos.message.onSpotifyAuthError(error = e)
            }
        }
    }

    fun playAlbum(albumId: String) = managers.player.playAlbum(albumId)

    fun playArtist(artistId: String) = managers.player.playArtist(artistId)

    fun playTrack(trackId: String) = managers.player.playTrack(trackId)

    fun setContentSize(size: Size) = repos.settings.setContentSize(size)

    fun setScreenSize(dpSize: DpSize, size: Size) = repos.settings.setScreenSize(dpSize, size)

    fun setWelcomeDialogShown(value: Boolean) = repos.settings.setWelcomeDialogShown(value)

    fun startAlbumRadio(albumId: String) = managers.radio.startAlbumRadio(albumId)

    fun startArtistRadio(artistId: String) = managers.radio.startArtistRadio(artistId)

    fun startTrackRadio(trackId: String) = managers.radio.startTrackRadio(trackId)
}
