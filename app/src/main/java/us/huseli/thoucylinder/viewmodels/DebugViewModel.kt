package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import us.huseli.retaintheme.extensions.slice
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTrack
import us.huseli.thoucylinder.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _spotifyArtistIds = MutableStateFlow<List<String>>(emptyList())
    private val _spotifyRecommendations = MutableStateFlow<List<SpotifyTrack>>(emptyList())
    private val _spotifyRelatedArtists = MutableStateFlow<List<SpotifyArtist>>(emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val albums = repos.album.listAlbums()
            val spotifyAlbumIds = albums.mapNotNull { it.spotifyId }.slice(0, 20)
            val spotifyAlbums = repos.spotify.getSpotifyAlbums(spotifyAlbumIds)

            spotifyAlbums?.albums
                ?.flatMap { album -> album.artists.map { it.id } }
                ?.also { _spotifyArtistIds.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _spotifyArtistIds.collect { artistIds ->
                repos.spotify.getTrackRecommendations(artistIds.slice(0, 5))
                    ?.tracks?.also { _spotifyRecommendations.value = it }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _spotifyArtistIds.collect { artistIds ->
                artistIds.slice(0, 5).forEach { artistId ->
                    repos.spotify.getRelatedArtists(artistId)?.artists?.also { _spotifyRelatedArtists.value += it }
                }
            }
        }
    }

    fun clearDatabase() = launchOnIOThread {
        repos.track.clearTracks()
        repos.album.clearAlbums()
        repos.album.clearTags()
        repos.artist.clearArtists()
        SnackbarEngine.addInfo("Cleared database.")
    }
}
