package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.toAlbumArtists
import us.huseli.thoucylinder.dataclasses.views.toTrackArtists
import us.huseli.thoucylinder.launchOnIOThread
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractAlbumListViewModel("YoutubeSearchViewModel", repos) {
    private val _isSearchingAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _albumCombos = MutableStateFlow<List<AlbumWithTracksCombo>>(emptyList())
    private val _trackCombos = MutableStateFlow<PagingData<TrackCombo>>(PagingData.empty())

    override val albumCombos = _albumCombos.asStateFlow()
    val trackCombos = _trackCombos.asStateFlow()
    val isSearchingTracks = repos.youtube.isSearchingTracks
    val isSearchingAlbums = _isSearchingAlbums.asStateFlow()
    val query = _query.asStateFlow()

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingAlbums.value = true

                launchOnIOThread {
                    val combos = repos.youtube.searchPlaylistCombos(query)
                        .map { playlistCombo ->
                            playlistCombo.toAlbumCombo(
                                isInLibrary = false,
                                getArtist = { repos.artist.artistCache.getByName(it) },
                            )
                        }

                    if (combos.isNotEmpty()) {
                        val tracks = combos.flatMap { it.trackCombos.map { trackCombo -> trackCombo.track } }
                        repos.album.upsertAlbumsAndTags(combos)
                        if (tracks.isNotEmpty()) repos.track.upsertTracks(tracks)
                        repos.artist.insertAlbumArtists(combos.flatMap { it.artists.toAlbumArtists() })
                        repos.artist.insertTrackArtists(
                            combos.flatMap { combo -> combo.trackCombos.flatMap { it.artists.toTrackArtists() } }
                        )
                    }
                    _albumCombos.value = combos
                    _isSearchingAlbums.value = false
                }

                launchOnIOThread {
                    repos.youtube.searchTracks(query).flow.cachedIn(viewModelScope).collectLatest { pagingData ->
                        _trackCombos.value = pagingData.map { TrackCombo(track = it) }
                    }
                }
            }
        }
    }

    fun updateFromMusicBrainzAsync(combo: AlbumWithTracksCombo) = launchOnIOThread { updateFromMusicBrainz(combo) }

    override fun onAllAlbumIds(callback: (Collection<UUID>) -> Unit) {
        callback(_albumCombos.value.map { it.album.albumId })
    }

    override fun onSelectedAlbumsWithTracks(callback: (Collection<AlbumWithTracksCombo>) -> Unit) {
        callback(_albumCombos.value.filter { selectedAlbumIds.value.contains(it.album.albumId) })
    }

    override fun onSelectedAlbumTracks(callback: (Collection<Track>) -> Unit) {
        callback(
            _albumCombos.value.filter { selectedAlbumIds.value.contains(it.album.albumId) }
                .flatMap { combo -> combo.trackCombos.map { it.track } }
        )
    }
}
