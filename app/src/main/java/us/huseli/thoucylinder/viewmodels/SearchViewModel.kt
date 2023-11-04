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
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.AlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

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
    private val _youtubeAlbums = MutableStateFlow<List<AlbumPojo>>(emptyList())
    private val _youtubeTracks = MutableStateFlow<PagingData<TrackPojo>>(PagingData.empty())

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

    val youtubeAlbums = _youtubeAlbums.asStateFlow()
    val youtubeTracksPaging = _youtubeTracks.asStateFlow()
    val selectedYoutubeAlbums = repos.room.getSelectedAlbumFlow("${javaClass.simpleName}-youtube")
    val selectedYoutubeTracks = repos.room.getSelectedTrackFlow("${javaClass.simpleName}-youtube")
    val isSearchingYoutubeTracks = repos.youtube.isSearchingTracks
    val isSearchingYoutubeAlbums = _isSearchingYoutubeAlbums.asStateFlow()
    val isSearchingLocalAlbums = _isSearchingLocalAlbums.asStateFlow()
    val isSearchingLocalTracks = _isSearchingLocalTracks.asStateFlow()

    suspend fun ensureVideoMetadata(pojos: List<TrackPojo>): List<TrackPojo> =
        pojos.map { pojo -> pojo.copy(track = repos.youtube.ensureVideoMetadata(pojo.track)) }

    suspend fun populateTempAlbums(albums: List<Album>): List<AlbumWithTracksPojo> {
        val pojos = mutableListOf<AlbumWithTracksPojo>()
        albums.forEach { album ->
            if (!repos.room.albumExists(album.albumId)) {
                val pojo = repos.youtube.populateAlbumTracks(album = album, withMetadata = true)
                repos.room.insertTempAlbumWithTracks(pojo)
                pojos.add(pojo)
            } else {
                pojos.add(repos.room.getAlbumWithTracks(album.albumId)!!)
            }
        }
        return pojos
    }

    fun search(query: String) {
        if (query != _query.value) {
            _query.value = query

            if (query.length >= 3) {
                _isSearchingLocalAlbums.value = true
                _isSearchingYoutubeAlbums.value = true

                viewModelScope.launch {
                    _youtubeAlbums.value = repos.youtube.getAlbumSearchResult(query)
                    _isSearchingYoutubeAlbums.value = false
                }
                viewModelScope.launch {
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

    fun selectLocalAlbumsFromLastSelected(to: Album) =
        selectAlbumsFromLastSelected(albums = _localAlbums.value.map { it.album }, to = to)

    fun selectYoutubeAlbumsFromLastSelected(to: Album) =
        selectAlbumsFromLastSelected(albums = _youtubeAlbums.value.map { it.album }, to = to)

    fun toggleSelectedYoutube(album: Album) = repos.room.toggleAlbumSelected("${javaClass.simpleName}-youtube", album)

    fun toggleSelectedYoutube(track: TrackPojo) =
        repos.room.toggleTrackSelected("${javaClass.simpleName}-youtube", track)

    fun unselectAllYoutubeAlbums() = repos.room.unselectAllAlbums("${javaClass.simpleName}-youtube")

    fun unselectAllYoutubeTracks() = repos.room.unselectAllTracks("${javaClass.simpleName}-youtube")
}
