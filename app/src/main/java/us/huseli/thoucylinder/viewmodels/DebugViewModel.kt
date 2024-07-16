package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AbstractYoutubeClient
import us.huseli.thoucylinder.dataclasses.youtube.getBest
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel(), ILogger {
    private val _continuationTokens = MutableStateFlow<Map<String, String>>(emptyMap())

    val continuationTokens = _continuationTokens.asStateFlow()
    val region = repos.settings.region

    fun clearDatabase() {
        launchOnIOThread {
            repos.track.clearTracks()
            repos.album.clearAlbums()
            repos.album.clearTags()
            repos.artist.clearArtists()
            SnackbarEngine.addInfo("Cleared database.")
        }
    }

    fun doStartupTasks() = managers.library.doStartupTasks()

    fun getMetadata(client: AbstractYoutubeClient) {
        launchOnIOThread {
            try {
                val list = client.getMetadata("1SuUKuOjq5c")
                val best = list.getBest()
                log("All metadatata: $list")
                log("Best metadata: $best")
            } catch (e: Exception) {
                logError(e)
            }
        }
    }

    fun getVideoSearchResultContinued(client: AbstractYoutubeClient, token: String) {
        launchOnIOThread {
            try {
                val result = client.getVideoSearchResult("roy harper hors d'oeuvres", token)
                log("getVideoSearchResultContinued(${client.clientName}): $result")
            } catch (e: Exception) {
                logError(e)
            }
        }
    }

    fun getVideoSearchResult(client: AbstractYoutubeClient) {
        launchOnIOThread {
            try {
                val result = client.getVideoSearchResult("roy harper hors d'oeuvres")
                log("getVideoSearchResult(${client.clientName}): $result")
                result.nextToken?.also { _continuationTokens.value += client.clientName to it }
            } catch (e: Exception) {
                logError(e)
            }
        }
    }

    fun searchPlaylistCombos(client: AbstractYoutubeClient) {
        launchOnIOThread {
            try {
                val combos = client.searchPlaylistCombos("roy harper stormcock")
                log("searchPlaylistCombos(${client.clientName}): $combos")
            } catch (e: Exception) {
                logError(e)
            }
        }
    }
}
