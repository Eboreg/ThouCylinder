package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.content.Intent
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
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.dataclasses.ImportProgressStatus
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.filterBySearchTerm
import us.huseli.thoucylinder.dataclasses.musicBrainz.artistString
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.lastFm.getFullImage
import us.huseli.thoucylinder.dataclasses.lastFm.getThumbnail
import us.huseli.thoucylinder.repositories.MusicBrainzRepository
import us.huseli.thoucylinder.umlautify
import java.util.UUID
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
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _progress = MutableStateFlow<ImportProgressData?>(null)
    private val _importedAlbumIds = MutableStateFlow<Map<String, UUID>>(emptyMap())

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
    val username: StateFlow<String?> = repos.lastFm.username
    val notFoundAlbumIds: StateFlow<List<String>> = _notFoundAlbumIds.asStateFlow()
    val isAllSelected: Flow<Boolean> = combine(offsetTopAlbums, _selectedTopAlbums) { userAlbums, selected ->
        userAlbums.isNotEmpty() && selected.map { it.mbid }.containsAll(userAlbums.map { it.mbid })
    }
    val progress = _progress.asStateFlow()
    val importedAlbumIds = _importedAlbumIds.asStateFlow()

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

    fun handleIntent(intent: Intent, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        intent.data?.getQueryParameter("token")?.also { authToken ->
            try {
                repos.lastFm.fetchSession(authToken)
            } catch (e: Exception) {
                repos.lastFm.setScrobble(false)
                Log.e(javaClass.simpleName, "handleIntent: $e", e)
                SnackbarEngine.addError(context.getString(R.string.last_fm_authorization_failed, e).umlautify())
            }
        }
    }

    fun importSelectedTopAlbums(context: Context, onFinish: (importedIds: List<UUID>, notFoundCount: Int) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val selectedTopAlbums = _selectedTopAlbums.value
            var notFoundCount = 0
            val importedIds = mutableListOf<UUID>()

            selectedTopAlbums.forEachIndexed { index, topAlbum ->
                val progressBaseline = index.toDouble() / selectedTopAlbums.size
                val progressMultiplier = 1.0 / selectedTopAlbums.size

                _progress.value = ImportProgressData(
                    item = topAlbum.name.umlautify(),
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

                    val albumArt = topAlbum.image.getFullImage()
                        ?.let { MediaStoreImage.fromUrls(it.url, topAlbum.image.getThumbnail()?.url ?: it.url) }
                        ?.saveInternal(combo.album, context)

                    repos.album.insertAlbumCombo(
                        combo.copy(album = combo.album.copy(albumArt = albumArt ?: combo.album.albumArt))
                    )
                    repos.track.insertTracks(combo.tracks)
                    updateProgressData(progress = progressBaseline + progressMultiplier)
                    _importedAlbumIds.value += topAlbum.mbid to combo.album.albumId
                    importedIds.add(combo.album.albumId)
                } else {
                    notFoundCount++
                    _notFoundAlbumIds.value += topAlbum.mbid
                }

                _selectedTopAlbums.value -= topAlbum
            }

            _progress.value = null
            if (selectedTopAlbums.isNotEmpty()) {
                onFinish(importedIds, notFoundCount)
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
            !_importedAlbumIds.value.containsKey(it.mbid) && !_notFoundAlbumIds.value.contains(it.mbid)
        }
        else _selectedTopAlbums.value = emptyList()
    }

    fun toggleSelected(album: LastFmTopAlbumsResponse.Album) {
        if (_selectedTopAlbums.value.contains(album)) _selectedTopAlbums.value -= album
        else if (!_importedAlbumIds.value.containsKey(album.mbid) && !_notFoundAlbumIds.value.contains(album.mbid)) {
            _selectedTopAlbums.value += album
        }
    }


    /** PRIVATE FUNCTIONS *****************************************************/
    private suspend fun findAlbumOnYoutube(
        musicBrainzId: String,
        progressCallback: (Double) -> Unit = {},
    ): AlbumWithTracksCombo? = repos.musicBrainz.getRelease(musicBrainzId)?.let { musicBrainzRelease ->
        val query = "${musicBrainzRelease.artistCredit.artistString()} - ${musicBrainzRelease.title}"

        repos.youtube.searchAlbumsWithTracks(query, progressCallback)
            .map { musicBrainzRelease.matchAlbumWithTracks(it) }
            .filter { it.score <= MusicBrainzRepository.MAX_ALBUM_MATCH_DISTANCE }
            .minByOrNull { it.score }
            ?.albumCombo
            ?.let { combo ->
                combo.copy(
                    album = combo.album.copy(isInLibrary = true),
                    tracks = combo.tracks.map { it.copy(isInLibrary = true) },
                )
            }
    }

    private fun updateProgressData(progress: Double? = null, status: ImportProgressStatus? = null) {
        _progress.value = _progress.value?.let {
            it.copy(progress = progress ?: it.progress, status = status ?: it.status)
        }
    }
}
