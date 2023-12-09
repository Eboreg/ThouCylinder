package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class YoutubeSearchViewModel @Inject constructor(
    private val repos: Repositories
) : AbstractAlbumListViewModel("YoutubeSearchViewModel", repos) {
    private val _isSearchingLocalAlbums = MutableStateFlow(false)
    private val _isSearchingYoutubeAlbums = MutableStateFlow(false)
    private val _query = MutableStateFlow("")
    private val _youtubeAlbumPojos = MutableStateFlow<List<AlbumWithTracksPojo>>(emptyList())
    private val _youtubeTrackPojos = MutableStateFlow<PagingData<TrackPojo>>(PagingData.empty())
    private val _selectedYoutubeAlbumPojos = MutableStateFlow<List<AlbumWithTracksPojo>>(emptyList())

    val youtubeAlbumPojos = _youtubeAlbumPojos.asStateFlow()
    val youtubeTrackPojos = _youtubeTrackPojos.asStateFlow()
    val selectedYoutubeAlbumPojos = _selectedYoutubeAlbumPojos.asStateFlow()
    val selectedYoutubeTrackPojos = repos.room.getSelectedTrackPojoFlow("${javaClass.simpleName}-youtube")
    val isSearchingYoutubeTracks = repos.youtube.isSearchingTracks
    val isSearchingYoutubeAlbums = _isSearchingYoutubeAlbums.asStateFlow()
    val query = _query.asStateFlow()

    suspend fun ensureTrackMetadata(pojos: List<TrackPojo>): List<TrackPojo> =
        pojos.map { pojo -> pojo.copy(track = ensureTrackMetadata(pojo.track, commit = true)) }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingLocalAlbums.value = true
                _isSearchingYoutubeAlbums.value = true

                viewModelScope.launch {
                    val pojos = repos.youtube.getAlbumSearchResult(query)

                    repos.room.insertTempAlbumsWithTracks(pojos)
                    _youtubeAlbumPojos.value = pojos
                    _isSearchingYoutubeAlbums.value = false
                }
                viewModelScope.launch {
                    repos.youtube.searchTracks(query).flow.map { pagingData ->
                        pagingData.map { TrackPojo(track = it, album = null) }
                    }.cachedIn(viewModelScope).collectLatest {
                        _youtubeTrackPojos.value = it
                    }
                }
            }
        }
    }

    fun selectAllYoutubeAlbumPojos() {
        _selectedYoutubeAlbumPojos.value = _youtubeAlbumPojos.value
    }

    fun selectYoutubeAlbumPojosFromLastSelected(to: AlbumWithTracksPojo) {
        val pojos = _youtubeAlbumPojos.value
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

    fun toggleSelectedYoutube(pojo: AlbumWithTracksPojo) {
        if (_selectedYoutubeAlbumPojos.value.contains(pojo))
            _selectedYoutubeAlbumPojos.value -= pojo
        else _selectedYoutubeAlbumPojos.value += pojo
    }

    fun toggleSelectedYoutube(track: TrackPojo) =
        repos.room.toggleTrackPojoSelected("${javaClass.simpleName}-youtube", track)

    fun unselectAllYoutubeAlbumPojos() {
        _selectedYoutubeAlbumPojos.value = emptyList()
    }

    fun unselectAllYoutubeTrackPojos() = repos.room.unselectAllTrackPojos("${javaClass.simpleName}-youtube")
}
