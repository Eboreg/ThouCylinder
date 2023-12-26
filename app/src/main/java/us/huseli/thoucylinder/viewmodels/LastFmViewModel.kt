package us.huseli.thoucylinder.viewmodels

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
import us.huseli.thoucylinder.capitalized
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.dataclasses.ImportProgressStatus
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.LastFmAlbum
import us.huseli.thoucylinder.dataclasses.entities.LastFmTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.lastFm.filterBySearchTerm
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.LastFmAlbumPojo
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class LastFmAlbumMatch(
    val albumWithTracksPojo: AlbumWithTracksPojo,
    val lastFmAlbum: LastFmAlbum,
    val youtubeAlbumPojo: AlbumWithTracksPojo,
    val levenshtein: Double,
)

@HiltViewModel
class LastFmViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _offset = MutableStateFlow(0)
    private val _searchTerm = MutableStateFlow("")
    private val _filteredTopAlbums: Flow<List<LastFmTopAlbumsResponse.TopAlbums.Album>> =
        combine(repos.lastFm.topAlbums, _searchTerm) { albums, term ->
            albums.filterBySearchTerm(term).filter { !_pastImportedAlbumIds.contains(it.mbid) }
        }
    private val _isSearching = MutableStateFlow(false)
    private val _selectedTopAlbums = MutableStateFlow<List<LastFmTopAlbumsResponse.TopAlbums.Album>>(emptyList())
    private val _importedAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _progress = MutableStateFlow<ImportProgressData?>(null)
    private var _genres: List<Genre>? = null

    val hasNext: Flow<Boolean> =
        combine(_filteredTopAlbums, repos.lastFm.allTopAlbumsFetched, _offset) { albums, allFetched, offset ->
            (!allFetched && albums.isNotEmpty()) || albums.size >= offset + 50
        }
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val offset: StateFlow<Int> = _offset.asStateFlow()
    val offsetTopAlbums: Flow<List<LastFmTopAlbumsResponse.TopAlbums.Album>> =
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
        viewModelScope.launch {
            _pastImportedAlbumIds.addAll(repos.lastFm.listImportedAlbumIds())
        }
    }

    private fun fetchTopAlbumsIfNeeded() = viewModelScope.launch {
        while (_filteredTopAlbums.first().size <= _offset.value + 100) {
            _isSearching.value = true
            if (!repos.lastFm.fetchNextTopAlbums()) break
        }
        _isSearching.value = false
    }

    suspend fun getAlbumArt(album: LastFmTopAlbumsResponse.TopAlbums.Album) = repos.lastFm.getThumbnail(album)

    fun getSessionKey(authToken: String, onError: (Exception) -> Unit = {}) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repos.lastFm.getSession(authToken)?.also {
                repos.settings.setLastFmSessionKey(it.key)
                repos.settings.setLastFmUsername(it.name)
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun updateProgressData(item: String? = null, progress: Double? = null, status: ImportProgressStatus? = null) {
        _progress.value = _progress.value?.let {
            it.copy(item = item ?: it.item, progress = progress ?: it.progress, status = status ?: it.status)
        }
    }

    fun importSelectedTopAlbums(onFinish: (importCount: Int, notFoundCount: Int) -> Unit) = viewModelScope.launch {
        val selectedTopAlbums = _selectedTopAlbums.value
        val lastFmAlbumPairs = selectedTopAlbums
            .mapNotNull { topAlbum -> repos.lastFm.topAlbumToAlbum(topAlbum)?.let { Pair(topAlbum, it) } }
        val allGenres = _genres ?: repos.album.listGenres().also { _genres = it }
        var notFoundCount = 0

        lastFmAlbumPairs.forEachIndexed { index, (lastFmTopAlbum, lastFmAlbum) ->
            val progressBaseline = index.toDouble() / lastFmAlbumPairs.size
            val progressMultiplier = 1.0 / lastFmAlbumPairs.size

            _progress.value = ImportProgressData(
                item = lastFmAlbum.name,
                progress = progressBaseline,
                status = ImportProgressStatus.MATCHING,
            )

            val match = findAlbumOnYoutube(lastFmAlbum, allGenres) { progress ->
                updateProgressData(progress = progressBaseline + (progress * progressMultiplier * 0.9))
            }

            if (match != null) {
                updateProgressData(
                    progress = progressBaseline + (progressMultiplier * 0.9),
                    status = ImportProgressStatus.IMPORTING,
                )
                repos.album.saveAlbumPojo(match.albumWithTracksPojo)
                repos.track.insertTracks(match.albumWithTracksPojo.tracks)
                repos.lastFm.insertLastFmAlbumPojo(
                    LastFmAlbumPojo(
                        album = match.lastFmAlbum,
                        tracks = match.lastFmAlbum.tracks.mapIndexedNotNull { trackIdx, lfmTrack ->
                            match.albumWithTracksPojo.tracks.getOrNull(trackIdx)?.let { track ->
                                LastFmTrack(
                                    duration = lfmTrack.duration,
                                    url = lfmTrack.url,
                                    name = lfmTrack.name,
                                    artist = lfmTrack.artist,
                                    trackId = track.trackId,
                                    albumId = match.lastFmAlbum.musicBrainzId,
                                    musicBrainzId = lfmTrack.mbid,
                                )
                            }
                        }
                    )
                )
                updateProgressData(progress = progressBaseline + progressMultiplier)
                _importedAlbumIds.value += lastFmTopAlbum.mbid
            } else {
                notFoundCount++
                _notFoundAlbumIds.value += lastFmTopAlbum.mbid
            }

            _selectedTopAlbums.value -= lastFmTopAlbum
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

    fun toggleSelected(album: LastFmTopAlbumsResponse.TopAlbums.Album) {
        if (_selectedTopAlbums.value.contains(album)) _selectedTopAlbums.value -= album
        else if (!_importedAlbumIds.value.contains(album.mbid) && !_notFoundAlbumIds.value.contains(album.mbid)) {
            _selectedTopAlbums.value += album
        }
    }


    /** PRIVATE FUNCTIONS *****************************************************/
    private suspend fun findAlbumOnYoutube(
        lastFmAlbum: LastFmAlbum,
        allGenres: List<Genre>,
        progressCallback: (Double) -> Unit = {},
    ): LastFmAlbumMatch? {
        val albumId = UUID.randomUUID()
        val tags = lastFmAlbum.tags.map { it.capitalized() }
        val genres = allGenres.filter { tags.contains(it.genreName) }
        val pojo = AlbumWithTracksPojo(
            album = Album(
                title = lastFmAlbum.name,
                isLocal = false,
                isInLibrary = true,
                year = lastFmAlbum.year,
                artist = lastFmAlbum.artist.name,
                albumId = albumId,
                lastFmFullImageUrl = lastFmAlbum.fullImageUrl,
                lastFmThumbnailUrl = lastFmAlbum.thumbnailUrl,
            ),
            genres = genres,
            lastFmAlbum = lastFmAlbum.copy(albumId = albumId),
            tracks = lastFmAlbum.tracks.mapIndexed { index, lastFmTrack ->
                Track(
                    albumId = albumId,
                    artist = lastFmTrack.artist.name,
                    isInLibrary = true,
                    albumPosition = index + 1,
                    title = lastFmTrack.name,
                )
            },
        )

        return repos.youtube.getAlbumSearchResult("${lastFmAlbum.artist.name} - ${lastFmAlbum.name}", progressCallback)
            .asSequence()
            .map {
                if (it.tracks.size > lastFmAlbum.tracks.size)
                    it.copy(tracks = it.tracks.subList(0, lastFmAlbum.tracks.size))
                else it
            }
            .map {
                LastFmAlbumMatch(
                    albumWithTracksPojo = pojo.copy(album = pojo.album.copy(youtubePlaylist = it.album.youtubePlaylist)),
                    lastFmAlbum = lastFmAlbum,
                    youtubeAlbumPojo = it,
                    levenshtein = pojo.getLevenshteinDistance(it),
                )
            }
            .filter { it.levenshtein < 10.0 }
            .sortedBy { it.levenshtein }
            .minByOrNull { it.youtubeAlbumPojo.tracks.size <= it.lastFmAlbum.tracks.size }
            ?.let { match ->
                val tracks = match.youtubeAlbumPojo.tracks.mapIndexed { trackIdx, trackYoutube ->
                    val trackLastFm = match.albumWithTracksPojo.tracks.getOrNull(trackIdx)
                    val (title, artist) =
                        if (
                            trackLastFm != null &&
                            trackLastFm.getLevenshteinDistance(
                                trackYoutube,
                                match.albumWithTracksPojo.album.artist
                            ) <= 10
                        ) Pair(trackLastFm.title, trackLastFm.artist)
                        else Pair(trackYoutube.title, trackYoutube.artist)

                    trackYoutube.copy(
                        title = title,
                        artist = artist,
                        isInLibrary = true,
                        albumId = match.albumWithTracksPojo.album.albumId,
                    )
                }

                match.copy(
                    albumWithTracksPojo = match.albumWithTracksPojo.copy(tracks = tracks),
                    lastFmAlbum = match.lastFmAlbum.copy(albumId = match.albumWithTracksPojo.album.albumId),
                )
            }
    }
}
