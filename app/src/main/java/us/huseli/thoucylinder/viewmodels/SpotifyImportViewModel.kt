package us.huseli.thoucylinder.viewmodels

import android.app.Activity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.dataclasses.ImportProgressStatus
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyAlbumPojo
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import kotlin.math.min

data class AlbumMatch(
    val albumWithTracksPojo: AlbumWithTracksPojo,
    val spotifyAlbumPojo: SpotifyAlbumPojo,
    val youtubeAlbumPojo: AlbumWithTracksPojo,
    val levenshtein: Double,
)

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    private val _lastFetchedIdx = MutableStateFlow(0)
    private val _localOffset = MutableStateFlow(0)
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _pojos = MutableStateFlow<List<SpotifyAlbumPojo>>(emptyList())
    private val _selectedPojos = MutableStateFlow<List<SpotifyAlbumPojo>>(emptyList())
    private val _thumbnailCache = mutableMapOf<String, ImageBitmap>()
    private val _totalAlbumCount = MutableStateFlow(0)
    private val _progress = MutableStateFlow<ImportProgressData?>(null)
    private val _importedAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())

    val pojos: Flow<List<SpotifyAlbumPojo>> = combine(_pojos, _localOffset) { pojos, offset ->
        if (pojos.size > offset) pojos.subList(offset, min(offset + 50, pojos.size))
        else emptyList()
    }
    val offset = _localOffset.asStateFlow()
    val hasPrevious = _localOffset.map { it > 0 }
    val selectedPojos = _selectedPojos.asStateFlow()
    val isAllSelected = combine(pojos, _selectedPojos) { pojos, selectedPojos ->
        pojos.isNotEmpty() && selectedPojos.map { it.spotifyAlbum.id }.containsAll(pojos.map { it.spotifyAlbum.id })
    }
    val isAuthorized = repos.spotify.isAuthorized
    val totalAlbumCount = _totalAlbumCount.asStateFlow()
    val hasNext = combine(_totalAlbumCount, _localOffset) { total, offset -> total > offset + 50 }
    val progress = _progress.asStateFlow()
    val importedAlbumIds = _importedAlbumIds.asStateFlow()
    val notFoundAlbumIds = _notFoundAlbumIds.asStateFlow()

    init {
        viewModelScope.launch {
            _pastImportedAlbumIds.addAll(repos.spotify.listImportedAlbumIds())
        }
    }

    fun authorize(activity: Activity) = repos.spotify.authorize(activity)

    fun fetchAlbums(offset: Int) = viewModelScope.launch(Dispatchers.IO) {
        _localOffset.value = offset
        _selectedPojos.value = emptyList()

        while (_pojos.value.size < offset + 100) {
            val response = repos.spotify.fetchUserAlbums(_lastFetchedIdx.value)

            if (response != null) {
                val pojos = response.items.map { it.toSpotifyAlbumPojo() }
                    .filter { !_pastImportedAlbumIds.contains(it.spotifyAlbum.id) }
                _lastFetchedIdx.value += response.items.size
                _pojos.value += pojos
                _totalAlbumCount.value = response.total - _pastImportedAlbumIds.size
            }
        }
    }

    suspend fun getThumbnail(spotifyAlbum: SpotifyAlbum): ImageBitmap? {
        _thumbnailCache[spotifyAlbum.id]?.also { return it }
        return spotifyAlbum.thumbnail?.getImageBitmap()
            ?.also { _thumbnailCache[spotifyAlbum.id] = it }
    }

    fun importSelectedAlbums() = viewModelScope.launch {
        val selectedPojos = _selectedPojos.value

        selectedPojos.forEachIndexed { index, spotifyPojo ->
            val importProgressData = ImportProgressData(
                item = spotifyPojo.spotifyAlbum.name,
                progress = index.toDouble() / selectedPojos.size,
                status = ImportProgressStatus.MATCHING,
            )
            _progress.value = importProgressData

            val match = findAlbumOnYoutube(spotifyPojo)

            if (match != null) {
                _progress.value = importProgressData.copy(
                    progress = importProgressData.progress + (0.5 / selectedPojos.size),
                    status = ImportProgressStatus.IMPORTING,
                )

                // Main "source of truth" for each track must be Youtube, since
                // that's where we get the audio from. But if the titles are
                // similar enough, we copy Spotify's track info as it's
                // generally better.
                val tracks = match.youtubeAlbumPojo.tracks.mapIndexed { trackIdx, trackYoutube ->
                    val trackSpotify = match.albumWithTracksPojo.tracks.getOrNull(trackIdx)

                    if (
                        trackSpotify != null &&
                        trackSpotify.getLevenshteinDistance(
                            trackYoutube,
                            match.albumWithTracksPojo.album.artist
                        ) <= 10
                    ) trackYoutube.copy(title = trackSpotify.title, artist = trackSpotify.artist)
                    else trackYoutube
                }
                val spotifyTrackPojos =
                    match.spotifyAlbumPojo.spotifyTrackPojos.mapIndexed { trackIdx, spotifyTrackPojo ->
                        tracks.getOrNull(trackIdx)
                            ?.let { spotifyTrackPojo.copy(track = spotifyTrackPojo.track.copy(trackId = it.trackId)) }
                            ?: spotifyTrackPojo
                    }
                val spotifyAlbum = match.spotifyAlbumPojo.spotifyAlbum.copy(
                    albumId = match.albumWithTracksPojo.album.albumId
                )
                val albumPojo = match.youtubeAlbumPojo.copy(
                    album = match.albumWithTracksPojo.album.copy(
                        youtubePlaylist = match.youtubeAlbumPojo.album.youtubePlaylist,
                    ),
                    tracks = tracks,
                    spotifyAlbum = spotifyAlbum,
                )
                val spotifyAlbumPojo = match.spotifyAlbumPojo.copy(
                    spotifyAlbum = spotifyAlbum,
                    spotifyTrackPojos = spotifyTrackPojos,
                )

                repos.room.saveAlbumWithTracks(albumPojo)
                repos.spotify.saveSpotifyAlbumPojo(spotifyAlbumPojo)

                _progress.value = importProgressData.copy(
                    progress = importProgressData.progress + (1 / selectedPojos.size),
                    status = ImportProgressStatus.IMPORTING,
                )
                _importedAlbumIds.value += spotifyPojo.spotifyAlbum.id
            } else {
                _notFoundAlbumIds.value += spotifyPojo.spotifyAlbum.id
            }
            _selectedPojos.value -= spotifyPojo
        }
        _progress.value = null
    }

    fun isSelected(spotifyPojo: SpotifyAlbumPojo) = _selectedPojos.value.contains(spotifyPojo)

    fun setAuthorizationResponse(value: AuthorizationResponse) {
        repos.spotify.setAuthorizationResponse(value)
    }

    fun setSelectAll(value: Boolean) = viewModelScope.launch {
        if (value) _selectedPojos.value = pojos.first().filter {
            !_importedAlbumIds.value.contains(it.spotifyAlbum.id) &&
                !_notFoundAlbumIds.value.contains(it.spotifyAlbum.id)
        }
        else _selectedPojos.value = emptyList()
    }

    fun toggleSelected(spotifyPojo: SpotifyAlbumPojo) {
        if (_selectedPojos.value.contains(spotifyPojo)) _selectedPojos.value -= spotifyPojo
        else if (
            !_importedAlbumIds.value.contains(spotifyPojo.spotifyAlbum.id) &&
            !_notFoundAlbumIds.value.contains(spotifyPojo.spotifyAlbum.id)
        ) _selectedPojos.value += spotifyPojo
    }

    private suspend fun findAlbumOnYoutube(spotifyPojo: SpotifyAlbumPojo): AlbumMatch? {
        val pojo = spotifyPojo.toAlbumPojo(isInLibrary = false)
        // First sort by Levenshtein distance, then put those with equal or
        // higher track count than pojo in front. #1 should be best match:
        return repos.youtube.getAlbumSearchResult(spotifyPojo.searchQuery)
            .asSequence()
            .map {
                if (it.tracks.size > spotifyPojo.spotifyTrackPojos.size)
                    it.copy(tracks = it.tracks.subList(0, spotifyPojo.spotifyTrackPojos.size))
                else it
            }
            .map {
                AlbumMatch(
                    spotifyAlbumPojo = spotifyPojo,
                    youtubeAlbumPojo = it,
                    levenshtein = pojo.getLevenshteinDistance(it),
                    albumWithTracksPojo = pojo,
                )
            }
            .filter { it.levenshtein < 10.0 }
            .sortedBy { it.levenshtein }
            .minByOrNull { it.youtubeAlbumPojo.tracks.size <= it.spotifyAlbumPojo.spotifyTrackPojos.size }
    }
}
