package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractAlbumListViewModel("YoutubeSearchViewModel", repos) {
    private val _isSearchingAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _albumCombos = MutableStateFlow<List<AlbumWithTracksCombo>>(emptyList())
    private val _trackCombos = MutableStateFlow<PagingData<TrackCombo>>(PagingData.empty())

    val albumCombos = _albumCombos.asStateFlow()
    val trackCombos = _trackCombos.asStateFlow()
    val isSearchingTracks = repos.youtube.isSearchingTracks
    val isSearchingAlbums = _isSearchingAlbums.asStateFlow()
    val query = _query.asStateFlow()
    val selectedAlbumsWithTracks: Flow<List<AlbumWithTracksCombo>> =
        combine(selectedAlbums, _albumCombos) { selected, combos ->
            combos.filter { combo -> selected.map { it.albumId }.contains(combo.album.albumId) }
        }

    fun updateFromMusicBrainz(combo: AlbumWithTracksCombo) = viewModelScope.launch(Dispatchers.IO) {
        if (combo.album.musicBrainzReleaseId == null) {
            val match = repos.musicBrainz.matchAlbumWithTracks(combo)

            if (match != null) {
                repos.album.updateAlbumCombo(match)
                repos.track.updateTracks(match.tracks)
                if (match.album.musicBrainzReleaseId != null) {
                    repos.musicBrainz.getReleaseCoverArt(match.album.musicBrainzReleaseId)?.also {
                        repos.album.updateAlbumArt(match.album.albumId, it)
                    }
                }
            }
        }
    }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingAlbums.value = true

                viewModelScope.launch(Dispatchers.IO) {
                    val combos = repos.youtube.searchAlbumsWithTracks(query)

                    repos.album.insertAlbumCombos(combos)
                    repos.track.insertTracks(combos.flatMap { it.tracks })
                    _albumCombos.value = combos
                    _isSearchingAlbums.value = false
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repos.youtube.searchTracks(query).flow.cachedIn(viewModelScope).collectLatest { pagingData ->
                        _trackCombos.value = pagingData.map { TrackCombo(track = it) }
                    }
                }
            }
        }
    }
}
