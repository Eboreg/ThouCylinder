package us.huseli.thoucylinder.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.dataclasses.ImportProgressStatus
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.filterBySearchTerm
import us.huseli.thoucylinder.dataclasses.musicBrainz.artistString
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class LastFmViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _offset = MutableStateFlow(0)
    private val _searchTerm = MutableStateFlow("")
    private val _filteredTopAlbums: Flow<List<LastFmTopAlbumsResponse.Album>> =
        combine(repos.lastFm.topAlbums, _searchTerm) { albums, term ->
            albums.filterBySearchTerm(term).filter { !_pastImportedAlbumIds.contains(it.mbid) }
        }
    private val _isSearching = MutableStateFlow(false)
    private val _selectedTopAlbums = MutableStateFlow<List<LastFmTopAlbumsResponse.Album>>(emptyList())
    private val _importedAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _progress = MutableStateFlow<ImportProgressData?>(null)

    val hasNext: Flow<Boolean> =
        combine(_filteredTopAlbums, repos.lastFm.allTopAlbumsFetched, _offset) { albums, allFetched, offset ->
            (!allFetched && albums.isNotEmpty()) || albums.size >= offset + 50
        }
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val offset: StateFlow<Int> = _offset.asStateFlow()
    val offsetTopAlbums: Flow<List<LastFmTopAlbumsResponse.Album>> =
        combine(_filteredTopAlbums, _offset) { albums, offset ->
            albums.subList(min(offset, max(albums.lastIndex, 0)), min(offset + 50, albums.size))
        }
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()
    val selectedTopAlbums = _selectedTopAlbums.asStateFlow()
    val totalFilteredAlbumCount: Flow<Int> = _filteredTopAlbums.map { it.size }
    val username: StateFlow<String?> = repos.settings.lastFmUsername
    val importedAlbumIds: StateFlow<List<String>> = _importedAlbumIds.asStateFlow()
    val notFoundAlbumIds: StateFlow<List<String>> = _notFoundAlbumIds.asStateFlow()
    val isAllSelected: Flow<Boolean> = combine(offsetTopAlbums, _selectedTopAlbums) { userAlbums, selected ->
        userAlbums.isNotEmpty() && selected.map { it.mbid }.containsAll(userAlbums.map { it.mbid })
    }
    val progress = _progress.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _pastImportedAlbumIds.addAll(repos.lastFm.listImportedAlbumIds())
        }
    }

    private fun fetchTopAlbumsIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        while (_filteredTopAlbums.first().size <= _offset.value + 100) {
            _isSearching.value = true
            if (!repos.lastFm.fetchNextTopAlbums()) break
        }
        _isSearching.value = false
    }

    suspend fun getAlbumArt(album: LastFmTopAlbumsResponse.Album) = repos.lastFm.getThumbnail(album)

    fun getSessionKey(authToken: String, onError: (Exception) -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repos.lastFm.getSession(authToken)?.also {
                repos.settings.setLastFmSessionKey(it.key)
                repos.settings.setLastFmUsername(it.name)
                repos.settings.setLastFmScrobble(true)
            }
        } catch (e: Exception) {
            repos.settings.setLastFmScrobble(false)
            Log.e(javaClass.simpleName, "getSessionKey: $e", e)
            onError(e)
        }
    }

    private fun updateProgressData(progress: Double? = null, status: ImportProgressStatus? = null) {
        _progress.value = _progress.value?.let {
            it.copy(progress = progress ?: it.progress, status = status ?: it.status)
        }
    }

    fun importSelectedTopAlbums(onFinish: (importCount: Int, notFoundCount: Int) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val selectedTopAlbums = _selectedTopAlbums.value
            var notFoundCount = 0

            selectedTopAlbums.forEachIndexed { index, topAlbum ->
                val progressBaseline = index.toDouble() / selectedTopAlbums.size
                val progressMultiplier = 1.0 / selectedTopAlbums.size

                _progress.value = ImportProgressData(
                    item = topAlbum.name,
                    progress = progressBaseline,
                    status = ImportProgressStatus.MATCHING,
                )

                val combo = findAlbumOnYoutube(topAlbum.mbid) { progress ->
                    updateProgressData(progress = progressBaseline + (progress * progressMultiplier * 0.9))
                }

                if (combo != null) {
                    updateProgressData(
                        progress = progressBaseline + (progressMultiplier * 0.9),
                        status = ImportProgressStatus.IMPORTING,
                    )
                    repos.album.saveAlbumCombo(combo)
                    repos.track.insertTracks(combo.tracks)
                    updateProgressData(progress = progressBaseline + progressMultiplier)
                    _importedAlbumIds.value += topAlbum.mbid
                } else {
                    notFoundCount++
                    _notFoundAlbumIds.value += topAlbum.mbid
                }

                _selectedTopAlbums.value -= topAlbum
            }

            _progress.value = null
            if (selectedTopAlbums.isNotEmpty()) {
                onFinish(selectedTopAlbums.size - notFoundCount, notFoundCount)
            }
        }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            setOffset(0)
        }
    }

    fun setOffset(value: Int) {
        _offset.value = value
        _selectedTopAlbums.value = emptyList()
        fetchTopAlbumsIfNeeded()
    }

    fun setSelectAll(value: Boolean) = viewModelScope.launch {
        if (value) _selectedTopAlbums.value = offsetTopAlbums.first().filter {
            !_importedAlbumIds.value.contains(it.mbid) && !_notFoundAlbumIds.value.contains(it.mbid)
        }
        else _selectedTopAlbums.value = emptyList()
    }

    fun toggleSelected(album: LastFmTopAlbumsResponse.Album) {
        if (_selectedTopAlbums.value.contains(album)) _selectedTopAlbums.value -= album
        else if (!_importedAlbumIds.value.contains(album.mbid) && !_notFoundAlbumIds.value.contains(album.mbid)) {
            _selectedTopAlbums.value += album
        }
    }


    /** PRIVATE FUNCTIONS *****************************************************/
    private suspend fun findAlbumOnYoutube(
        musicBrainzId: String,
        progressCallback: (Double) -> Unit = {},
    ): AlbumWithTracksCombo? = repos.musicBrainz.getRelease(musicBrainzId)?.let { musicBrainzRelease ->
        val query = "${musicBrainzRelease.artistCredit.artistString()} - ${musicBrainzRelease.title}"

        repos.youtube.getAlbumSearchResult(query, progressCallback)
            .map { repos.musicBrainz.matchAlbumWithTracks(it, musicBrainzRelease) }
            .filter { it.distance <= 1.0 }
            .minByOrNull { it.distance }
            ?.albumCombo
            ?.let { combo ->
                combo.copy(
                    album = combo.album.copy(isInLibrary = true),
                    tracks = combo.tracks.map { it.copy(isInLibrary = true) },
                )
            }
    }
}
