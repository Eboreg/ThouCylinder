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
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import javax.inject.Inject

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractAlbumListViewModel("YoutubeSearchViewModel", repos) {
    private val _isSearchingAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _albumPojos = MutableStateFlow<List<AlbumWithTracksPojo>>(emptyList())
    private val _trackPojos = MutableStateFlow<PagingData<TrackPojo>>(PagingData.empty())

    val albumPojos = _albumPojos.asStateFlow()
    val trackPojos = _trackPojos.asStateFlow()
    val isSearchingTracks = repos.youtube.isSearchingTracks
    val isSearchingAlbums = _isSearchingAlbums.asStateFlow()
    val query = _query.asStateFlow()
    val selectedAlbumsWithTracks: Flow<List<AlbumWithTracksPojo>> =
        combine(selectedAlbums, _albumPojos) { selected, pojos ->
            pojos.filter { pojo -> selected.map { it.albumId }.contains(pojo.album.albumId) }
        }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingAlbums.value = true

                viewModelScope.launch(Dispatchers.IO) {
                    val pojos = repos.youtube.getAlbumSearchResult(query)

                    repos.album.insertAlbums(pojos.map { it.album })
                    repos.track.insertTracks(pojos.flatMap { it.tracks })
                    _albumPojos.value = pojos
                    _isSearchingAlbums.value = false
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repos.youtube.searchTracks(query).flow.cachedIn(viewModelScope).collectLatest { pagingData ->
                        _trackPojos.value = pagingData.map { TrackPojo(track = it) }
                    }
                }
            }
        }
    }
}
