package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repos: Repositories
) : AbstractSelectViewModel("SearchViewModel", repos) {
    private val _isSearchingLocalAlbums = MutableStateFlow(false)
    private val _isSearchingLocalTracks = MutableStateFlow(false)
    private val _isSearchingYoutubeAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _filteredQuery = _query.filter { it.length >= 3 }.distinctUntilChanged()
    private val _localAlbums = MutableStateFlow<List<AlbumPojo>>(emptyList())
    private val _youtubeAlbums = MutableStateFlow<List<AlbumWithTracksPojo>>(emptyList())
    private val _youtubeTracks = MutableStateFlow<PagingData<TrackPojo>>(PagingData.empty())
    private val _selectedYoutubeAlbumPojos = MutableStateFlow<List<AlbumWithTracksPojo>>(emptyList())
    private val _selectedLocalAlbumPojos = MutableStateFlow<List<AlbumPojo>>(emptyList())

    val localAlbumPojos = _filteredQuery.flatMapLatest { query ->
        _isSearchingLocalAlbums.value = true
        repos.room.searchAlbums(query).also { _isSearchingLocalAlbums.value = false }
    }.distinctUntilChanged()

    val localTracks: Flow<PagingData<TrackPojo>> = _filteredQuery.flatMapLatest { query ->
        _isSearchingLocalTracks.value = true
        repos.room.searchTracks(query).flow.cachedIn(viewModelScope).also {
            _isSearchingLocalTracks.value = false
        }
    }.distinctUntilChanged()

    val selectedLocalAlbumPojos = _selectedLocalAlbumPojos.asStateFlow()
    val youtubeAlbums = _youtubeAlbums.asStateFlow()
    val youtubeTracksPaging = _youtubeTracks.asStateFlow()
    val selectedYoutubeAlbumPojos = _selectedYoutubeAlbumPojos.asStateFlow()
    val selectedYoutubeTracks = repos.room.getSelectedTrackFlow("${javaClass.simpleName}-youtube")
    val isSearchingYoutubeTracks = repos.youtube.isSearchingTracks
    val isSearchingYoutubeAlbums = _isSearchingYoutubeAlbums.asStateFlow()
    val isSearchingLocalAlbums = _isSearchingLocalAlbums.asStateFlow()
    val isSearchingLocalTracks = _isSearchingLocalTracks.asStateFlow()

    suspend fun ensureVideoMetadata(pojos: List<TrackPojo>): List<TrackPojo> =
        pojos.map { pojo -> pojo.copy(track = repos.youtube.ensureVideoMetadata(pojo.track)) }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingLocalAlbums.value = true
                _isSearchingYoutubeAlbums.value = true

                viewModelScope.launch {
                    val pojos = repos.youtube.getAlbumSearchResult(query)
                    repos.room.insertTempAlbumsWithTracks(pojos)
                    _youtubeAlbums.value = pojos
                    _isSearchingYoutubeAlbums.value = false
                }
                viewModelScope.launch {
                    repos.youtube.searchTracks(query).flow.map { pagingData ->
                        pagingData.map { TrackPojo(track = it, album = null) }
                    }.cachedIn(viewModelScope).collectLatest {
                        _youtubeTracks.value = it
                    }
                }
            }
        }
    }

    fun selectAllLocalAlbums() {
        _selectedLocalAlbumPojos.value = _localAlbums.value
    }

    fun selectAllYoutubeAlbums() {
        _selectedYoutubeAlbumPojos.value = _youtubeAlbums.value
    }

    fun selectLocalAlbumsFromLastSelected(to: AlbumPojo) {
        val pojos = _localAlbums.value
        val lastSelected = _selectedLocalAlbumPojos.value.lastOrNull()

        if (lastSelected != null) {
            val thisIdx = pojos.indexOf(to)
            val lastSelectedIdx = pojos.indexOf(lastSelected)
            val currentIds = _selectedLocalAlbumPojos.value.map { it.album.albumId }
            val selection = pojos.subList(min(thisIdx, lastSelectedIdx), max(thisIdx, lastSelectedIdx) + 1)

            _selectedLocalAlbumPojos.value += selection.filter { !currentIds.contains(it.album.albumId) }
        } else {
            _selectedLocalAlbumPojos.value = listOf(to)
        }
    }

    fun selectYoutubeAlbumsFromLastSelected(to: AlbumWithTracksPojo) {
        val pojos = _youtubeAlbums.value
        val lastSelected = _selectedYoutubeAlbumPojos.value.lastOrNull()

        if (lastSelected != null) {
            val thisIdx = pojos.indexOf(to)
            val lastSelectedIdx = pojos.indexOf(lastSelected)
            val currentIds = _selectedYoutubeAlbumPojos.value.map { it.album.albumId }
            val selection = pojos.subList(min(thisIdx, lastSelectedIdx), max(thisIdx, lastSelectedIdx) + 1)

            _selectedYoutubeAlbumPojos.value += selection.filter { !currentIds.contains(it.album.albumId) }
        } else {
            _selectedYoutubeAlbumPojos.value = listOf(to)
        }
    }

    fun toggleSelectedLocal(pojo: AlbumPojo) {
        if (_selectedLocalAlbumPojos.value.contains(pojo))
            _selectedLocalAlbumPojos.value -= pojo
        else _selectedLocalAlbumPojos.value += pojo
    }

    fun toggleSelectedYoutube(pojo: AlbumWithTracksPojo) {
        if (_selectedYoutubeAlbumPojos.value.contains(pojo))
            _selectedYoutubeAlbumPojos.value -= pojo
        else _selectedYoutubeAlbumPojos.value += pojo
    }

    fun toggleSelectedYoutube(track: TrackPojo) =
        repos.room.toggleTrackSelected("${javaClass.simpleName}-youtube", track)

    fun unselectAllLocalAlbums() {
        _selectedLocalAlbumPojos.value = emptyList()
    }

    fun unselectAllYoutubeAlbums() {
        _selectedYoutubeAlbumPojos.value = emptyList()
    }

    fun unselectAllYoutubeTracks() = repos.room.unselectAllTracks("${javaClass.simpleName}-youtube")
}
