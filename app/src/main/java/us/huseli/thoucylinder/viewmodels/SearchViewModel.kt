package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val repos: Repositories) : BaseViewModel(repos) {
    private val _isSearchingLocalAlbums = MutableStateFlow(false)
    private val _isSearchingLocalTracks = MutableStateFlow(false)
    private val _isSearchingYoutubeAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _filteredQuery = _query.filter { it.length >= 3 }.distinctUntilChanged()
    private val _localAlbums = MutableStateFlow<List<AlbumPojo>>(emptyList())
    private val _youtubeAlbums = MutableStateFlow<List<AlbumPojo>>(emptyList())
    private val _youtubeTracks = MutableStateFlow<PagingData<TrackPojo>>(PagingData.empty())

    val isSearching: Flow<Boolean> = combine(
        _isSearchingYoutubeAlbums,
        repos.youtube.isSearchingTracks,
        _isSearchingLocalAlbums,
        _isSearchingLocalTracks,
    ) { s1, s2, s3, s4 ->
        s1 || s2 || s3 || s4
    }.distinctUntilChanged()

    val localAlbums = _localAlbums.asStateFlow()

    val localTracks: Flow<PagingData<TrackPojo>> = _filteredQuery.flatMapLatest { query ->
        _isSearchingLocalTracks.value = true
        repos.local.searchTracks(query).flow.cachedIn(viewModelScope).also {
            _isSearchingLocalTracks.value = false
        }
    }.distinctUntilChanged()

    val youtubeAlbums = _youtubeAlbums.asStateFlow()
    val youtubeTracksPaging = _youtubeTracks.asStateFlow()

    fun populateTempAlbum(pojo: AlbumPojo) {
        repos.local.addOrUpdateTempAlbum(AlbumWithTracksPojo(album = pojo.album))
        viewModelScope.launch(Dispatchers.IO) {
            val albumWithTracks = repos.youtube.populateAlbumTracks(album = pojo.album, withMetadata = false)
            repos.local.addOrUpdateTempAlbum(albumWithTracks)
        }
    }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingLocalAlbums.value = true
                _isSearchingYoutubeAlbums.value = true

                viewModelScope.launch(Dispatchers.IO) {
                    _localAlbums.value = repos.local.searchAlbums(query)
                    _isSearchingLocalAlbums.value = false
                }
                viewModelScope.launch(Dispatchers.IO) {
                    _youtubeAlbums.value = repos.youtube.getAlbumSearchResult(query)
                    _isSearchingYoutubeAlbums.value = false
                }
                viewModelScope.launch(Dispatchers.IO) {
                    repos.youtube.searchTracks(query).flow.map { pagingData ->
                        pagingData.map {
                            TrackPojo(track = it, album = null)
                        }
                    }.cachedIn(viewModelScope).collectLatest {
                        _youtubeTracks.value = it
                    }
                }
            }
        }
    }
}
