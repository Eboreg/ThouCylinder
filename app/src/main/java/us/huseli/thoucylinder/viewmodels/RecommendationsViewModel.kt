package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTopArtistMatch
import us.huseli.thoucylinder.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class RecommendationsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _spotifyRelatedArtistMatches = MutableStateFlow<List<SpotifyTopArtistMatch>>(emptyList())

    val spotifyRelatedArtistMatches = _spotifyRelatedArtistMatches.asStateFlow()

    fun getSpotifyRelatedArtists() = launchOnIOThread {
        val localArtists = repos.artist.listTopSpotifyArtists()

        _spotifyRelatedArtistMatches.value = repos.spotify.getTopRelatedArtists(localArtists)

        launch {
            // Update any matching artists in the DB with Spotify ID.
            val artists = repos.artist.listArtists()
            val updatedArtists = mutableListOf<Artist>()

            for (spotifyArtist in _spotifyRelatedArtistMatches.value.map { it.spotifyArtist }) {
                artists.find {
                    it.name.lowercase() == spotifyArtist.name.lowercase() && it.spotifyId != spotifyArtist.id
                }?.also { updatedArtists.add(it.copy(spotifyId = spotifyArtist.id)) }
            }
            if (updatedArtists.isNotEmpty()) repos.artist.updateArtists(updatedArtists)
        }
    }
}
