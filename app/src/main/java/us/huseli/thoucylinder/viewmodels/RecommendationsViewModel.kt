package us.huseli.thoucylinder.viewmodels

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyTopArtistMatch
import us.huseli.thoucylinder.dataclasses.spotify.getThumbnailImageBitmap
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class RecommendationsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _spotifyRelatedArtistMatches = MutableStateFlow<List<SpotifyTopArtistMatch>>(emptyList())
    private val existingArtists = MutableStateFlow<List<Artist>>(emptyList())
    private var existingArtistsFetched = false

    val spotifyRelatedArtistMatches =
        _spotifyRelatedArtistMatches.map { matches -> matches.sortedByDescending { it.score }.toImmutableList() }

    init {
        launchOnIOThread {
            repos.artist.artistsWithTracksOrAlbums.collect { artists ->
                existingArtists.value = artists
                existingArtistsFetched = true
            }
        }
    }

    fun getLastFmRelatedArtists() = launchOnIOThread {
        for (lastFmArtist in repos.lastFm.getTopArtists(10)) {
            repos.spotify.matchArtist(lastFmArtist.name)?.also {
                getRelatedArtists(it.id, lastFmArtist.name, lastFmArtist.playcount.toInt())
            }
        }
    }

    fun getLocalRelatedArtists() = launchOnIOThread {
        for (localArtist in repos.artist.listTopSpotifyArtists()) {
            getRelatedArtists(localArtist.spotifyId, localArtist.name, localArtist.trackCount)
        }
    }

    suspend fun getArtistThumbnail(spotifyArtist: SpotifyArtist, context: Context) = withContext(Dispatchers.IO) {
        spotifyArtist.images.getThumbnailImageBitmap(context)
    }

    private suspend fun getRelatedArtists(spotifyId: String, name: String, score: Int) {
        while (!existingArtistsFetched) delay(100L)
        repos.spotify.getRelatedArtists(spotifyId, 5)?.forEach { spotifyArtist ->
            if (!existingArtists.value.map { it.name }.contains(spotifyArtist.name)) {
                updateRelatedArtistMatches(spotifyArtist, name, score)
                launchOnIOThread { repos.artist.upsertSpotifyArtist(spotifyArtist) }
            }
        }
    }

    private fun updateRelatedArtistMatches(spotifyArtist: SpotifyArtist, name: String, score: Int) {
        _spotifyRelatedArtistMatches.value = _spotifyRelatedArtistMatches.value.toMutableList().apply {
            val oldMatch = find { it.spotifyArtist.id == spotifyArtist.id }

            if (oldMatch != null) {
                remove(oldMatch)
                add(
                    oldMatch.copy(
                        score = oldMatch.score + score,
                        artists = oldMatch.artists.plus(name),
                    )
                )
            } else add(
                SpotifyTopArtistMatch(
                    artists = setOf(name),
                    spotifyArtist = spotifyArtist,
                    score = score,
                )
            )
        }
    }
}
