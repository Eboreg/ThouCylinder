package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.ILogger
import us.huseli.thoucylinder.dataclasses.youtube.getBest
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.AbstractYoutubeClient
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos), ILogger {
    private val _spotifyArtistIds = MutableStateFlow<List<String>>(emptyList())
    private val _continuationTokens = MutableStateFlow<Map<String, String>>(emptyMap())

    val continuationTokens = _continuationTokens.asStateFlow()
    val region = repos.settings.region

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val albums = repos.album.listAlbums()
            val spotifyAlbumIds = albums.mapNotNull { it.spotifyId }.take(20)
            val spotifyAlbums = repos.spotify.getSpotifyAlbums(spotifyAlbumIds)

            spotifyAlbums
                ?.flatMap { album -> album.artists.map { it.id } }
                ?.also { _spotifyArtistIds.value = it }
        }
    }

    fun clearDatabase() = launchOnIOThread {
        repos.track.clearTracks()
        repos.album.clearAlbums()
        repos.album.clearTags()
        repos.artist.clearArtists()
        SnackbarEngine.addInfo("Cleared database.")
    }

    fun getMetadata(client: AbstractYoutubeClient) = launchOnIOThread {
        try {
            val list = client.getMetadata("1SuUKuOjq5c")
            val best = list.getBest()
            log("All metadatata: $list")
            log("Best metadata: $best")
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun getVideoSearchResultContinued(
        context: Context,
        client: AbstractYoutubeClient,
        token: String,
    ) = launchOnIOThread {
        try {
            val result = client.getVideoSearchResult(context, "roy harper hors d'oeuvres", token)
            log("getVideoSearchResultContinued(${client.clientName}): $result")
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun getVideoSearchResult(context: Context, client: AbstractYoutubeClient) = launchOnIOThread {
        try {
            val result = client.getVideoSearchResult(context, "roy harper hors d'oeuvres")
            log("getVideoSearchResult(${client.clientName}): $result")
            result.nextToken?.also { _continuationTokens.value += client.clientName to it }
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun searchPlaylistCombos(context: Context, client: AbstractYoutubeClient) = launchOnIOThread {
        try {
            val combos = client.searchPlaylistCombos(context, "roy harper stormcock")
            log("searchPlaylistCombos(${client.clientName}): $combos")
        } catch (e: Exception) {
            logError(e)
        }
    }
}
