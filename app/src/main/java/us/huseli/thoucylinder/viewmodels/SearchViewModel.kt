package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.AlbumPojo
import us.huseli.thoucylinder.dataclasses.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.repositories.LocalRepository
import us.huseli.thoucylinder.repositories.MediaStoreRepository
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.YoutubeRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youtubeRepo: YoutubeRepository,
    playerRepo: PlayerRepository,
    private val repo: LocalRepository,
    mediaStoreRepo: MediaStoreRepository,
) : BaseViewModel(repo, playerRepo, youtubeRepo, mediaStoreRepo) {
    private val _isSearchingLocalAlbums = MutableStateFlow(false)
    private val _isSearchingLocalTracks = MutableStateFlow(false)
    private val _isSearchingYoutubeAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _filteredQuery = _query.filter { it.length >= 3 }.distinctUntilChanged()
    private val _localAlbums = MutableStateFlow<List<AlbumPojo>>(emptyList())
    private val _youtubeAlbums = MutableStateFlow<List<AlbumPojo>>(emptyList())
    private val _youtubeTracks = MutableStateFlow<PagingData<Track>>(PagingData.empty())

    val isSearching: Flow<Boolean> = combine(
        _isSearchingYoutubeAlbums,
        youtubeRepo.isSearchingTracks,
        _isSearchingLocalAlbums,
        _isSearchingLocalTracks,
    ) { s1, s2, s3, s4 ->
        s1 || s2 || s3 || s4
    }.distinctUntilChanged()

    val localAlbums = _localAlbums.asStateFlow()

    val localTracks: Flow<PagingData<Track>> = _filteredQuery.flatMapLatest { query ->
        _isSearchingLocalTracks.value = true
        repo.searchTracks(query).flow.cachedIn(viewModelScope).also {
            _isSearchingLocalTracks.value = false
        }
    }.distinctUntilChanged()

    val youtubeAlbums = _youtubeAlbums.asStateFlow()
    val youtubeTracksPaging = _youtubeTracks.asStateFlow()

    /*
    val youtubeTracksPaging = _filteredQuery.flatMapLatest { query ->
        youtubeRepo.searchTracks(query).flow.cachedIn(viewModelScope)
    }.distinctUntilChanged()
     */

    fun populateTempAlbum(pojo: AlbumPojo) {
        repo.addOrUpdateTempAlbum(AlbumWithTracksPojo(album = pojo.album))
        viewModelScope.launch(Dispatchers.IO) {
            val albumWithTracks = youtubeRepo.populateAlbumTracks(album = pojo.album, withMetadata = false)
            repo.addOrUpdateTempAlbum(albumWithTracks)
        }
    }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingLocalAlbums.value = true
                _isSearchingYoutubeAlbums.value = true

                viewModelScope.launch(Dispatchers.IO) {
                    _localAlbums.value = repo.searchAlbums(query)
                    _isSearchingLocalAlbums.value = false
                }
                viewModelScope.launch(Dispatchers.IO) {
                    _youtubeAlbums.value = youtubeRepo.getAlbumSearchResult(query)
                    _isSearchingYoutubeAlbums.value = false
                }
                viewModelScope.launch(Dispatchers.IO) {
                    youtubeRepo.searchTracks(query).flow.cachedIn(viewModelScope).collectLatest {
                        _youtubeTracks.value = it
                    }
                }
            }
        }
    }
}
