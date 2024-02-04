package us.huseli.thoucylinder.viewmodels

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.spotify.sdk.android.auth.AuthorizationResponse
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
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.SpotifyAlbumPojo
import us.huseli.thoucylinder.dataclasses.pojos.filterBySearchTerm
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class SpotifyAlbumMatch(
    val albumWithTracksPojo: AlbumWithTracksPojo,
    val spotifyAlbumPojo: SpotifyAlbumPojo,
    val youtubeAlbumPojo: AlbumWithTracksPojo,
    val levenshtein: Double,
)

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _localOffset = MutableStateFlow(0)
    private val _selectedAlbumPojos = MutableStateFlow<List<SpotifyAlbumPojo>>(emptyList())
    private val _thumbnailCache = mutableMapOf<String, ImageBitmap>()
    private val _progress = MutableStateFlow<ImportProgressData?>(null)
    private val _importedAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _searchTerm = MutableStateFlow("")
    private val _albumPojos = repos.spotify.userAlbumPojos.map { pojos ->
        pojos.filter { !_pastImportedAlbumIds.contains(it.spotifyAlbum.id) }
    }
    private val _filteredAlbumPojos: Flow<List<SpotifyAlbumPojo>> =
        combine(_albumPojos, _searchTerm) { pojos, term -> pojos.filterBySearchTerm(term) }
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _isSearching = MutableStateFlow(false)

    val offsetAlbumPojos: Flow<List<SpotifyAlbumPojo>> = combine(_filteredAlbumPojos, _localOffset) { pojos, offset ->
        pojos.subList(min(offset, max(pojos.lastIndex, 0)), min(offset + 50, pojos.size))
    }

    val localOffset: StateFlow<Int> = _localOffset.asStateFlow()
    val selectedAlbumPojos: StateFlow<List<SpotifyAlbumPojo>> = _selectedAlbumPojos.asStateFlow()
    val isAllSelected: Flow<Boolean> = combine(offsetAlbumPojos, _selectedAlbumPojos) { userAlbums, selected ->
        userAlbums.isNotEmpty() && selected.map { it.spotifyAlbum.id }
            .containsAll(userAlbums.map { it.spotifyAlbum.id })
    }
    val isAuthorized: Flow<Boolean?> = repos.spotify.isAuthorized
    val progress: StateFlow<ImportProgressData?> = _progress.asStateFlow()
    val importedAlbumIds: StateFlow<List<String>> = _importedAlbumIds.asStateFlow()
    val nextAlbumIdx = repos.spotify.nextUserAlbumIdx
    val notFoundAlbumIds: StateFlow<List<String>> = _notFoundAlbumIds.asStateFlow()
    val requestCode = repos.spotify.requestCode
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()
    val filteredAlbumCount: Flow<Int?> = combine(
        _searchTerm,
        _filteredAlbumPojos,
        repos.spotify.totalUserAlbumCount,
    ) { term, filteredAlbums, totalCount ->
        if (term == "") totalCount?.minus(_pastImportedAlbumIds.size)
        else filteredAlbums.size
    }
    val isAlbumCountExact: Flow<Boolean> =
        combine(_searchTerm, repos.spotify.allUserAlbumsFetched) { term, allFetched ->
            term == "" || allFetched
        }
    val hasNext: Flow<Boolean> =
        combine(filteredAlbumCount, _localOffset) { total, offset -> total == null || total > offset + 50 }
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val totalAlbumCount: StateFlow<Int?> = repos.spotify.totalUserAlbumCount

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _pastImportedAlbumIds.addAll(repos.spotify.listImportedAlbumIds())
        }
    }

    fun authorize(activity: Activity) = repos.spotify.authorize(activity)

    suspend fun getThumbnail(spotifyAlbum: SpotifyAlbum, context: Context): ImageBitmap? {
        _thumbnailCache[spotifyAlbum.id]?.also { return it }
        return spotifyAlbum.getThumbnailImageBitmap(context)?.also { _thumbnailCache[spotifyAlbum.id] = it }
    }

    fun importSelectedAlbums(onFinish: (importCount: Int, notFoundCount: Int) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val selectedPojos = _selectedAlbumPojos.value
            var notFoundCount = 0

            selectedPojos.forEachIndexed { index, spotifyPojo ->
                val progressBaseline = index.toDouble() / selectedPojos.size
                val progressMultiplier = 1.0 / selectedPojos.size
                val importProgressData = ImportProgressData(
                    item = spotifyPojo.spotifyAlbum.name,
                    progress = progressBaseline,
                    status = ImportProgressStatus.MATCHING,
                )

                _progress.value = importProgressData

                val match = findAlbumOnYoutube(spotifyPojo) { progress ->
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + (progress * progressMultiplier * 0.9)
                    )
                }

                if (match != null) {
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + (progressMultiplier * 0.9),
                        status = ImportProgressStatus.IMPORTING,
                    )

                    // Main "source of truth" for each track must be Youtube, since
                    // that's where we get the audio from. But if the durations are
                    // close enough, we copy Spotify's track info as it's
                    // generally better.
                    val tracks = match.youtubeAlbumPojo.tracks.mapIndexed { trackIdx, trackYoutube ->
                        val spotifyTrackPojo = match.spotifyAlbumPojo.spotifyTrackPojos.getOrNull(trackIdx)
                        val (title, artist) =
                            if (spotifyTrackPojo?.hasSimilarDuration(trackYoutube) == true)
                                Pair(spotifyTrackPojo.track.name, spotifyTrackPojo.artist)
                            else Pair(trackYoutube.title, trackYoutube.artist)
                        trackYoutube.copy(title = title, artist = artist, isInLibrary = true)
                    }
                    val spotifyTrackPojos =
                        match.spotifyAlbumPojo.spotifyTrackPojos.mapIndexed { trackIdx, spotifyTrackPojo ->
                            tracks.getOrNull(trackIdx)
                                ?.let { spotifyTrackPojo.copy(track = spotifyTrackPojo.track.copy(trackId = it.trackId)) }
                                ?: spotifyTrackPojo
                        }
                    val imageUrl = match.spotifyAlbumPojo.spotifyAlbum.fullImage?.url
                        ?: match.youtubeAlbumPojo.album.youtubePlaylist?.fullImage?.url
                    val albumArt =
                        imageUrl?.let {
                            repos.localMedia.saveInternalAlbumArtFiles(
                                match.albumWithTracksPojo.album,
                                it
                            )
                        }
                    val albumPojo = match.youtubeAlbumPojo.copy(
                        album = match.albumWithTracksPojo.album.copy(
                            youtubePlaylist = match.youtubeAlbumPojo.album.youtubePlaylist,
                            albumArt = albumArt,
                        ),
                        tracks = tracks.map { it.copy(albumId = match.albumWithTracksPojo.album.albumId) },
                        spotifyAlbum = match.spotifyAlbumPojo.spotifyAlbum,
                    )
                    val spotifyAlbumPojo = match.spotifyAlbumPojo.copy(
                        spotifyTrackPojos = spotifyTrackPojos,
                    )

                    repos.album.saveAlbumPojo(albumPojo)
                    repos.track.insertTracks(albumPojo.tracks)
                    repos.spotify.saveSpotifyAlbumPojo(spotifyAlbumPojo)
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + progressMultiplier,
                        status = ImportProgressStatus.IMPORTING,
                    )
                    _importedAlbumIds.value += spotifyPojo.spotifyAlbum.id
                } else {
                    notFoundCount++
                    _notFoundAlbumIds.value += spotifyPojo.spotifyAlbum.id
                }

                _selectedAlbumPojos.value -= spotifyPojo
            }

            _progress.value = null
            if (selectedPojos.isNotEmpty()) {
                onFinish(selectedPojos.size - notFoundCount, notFoundCount)
            }
        }

    fun setAuthorizationResponse(value: AuthorizationResponse) {
        Log.i(javaClass.simpleName, "setAuthorizationResponse: type=${value.type}, error=${value.error}")
        repos.spotify.setAuthorizationResponse(value)
    }

    fun setOffset(offset: Int) {
        _localOffset.value = offset
        _selectedAlbumPojos.value = emptyList()
        fetchUserAlbumsIfNeeded()
    }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            setOffset(0)
        }
    }

    fun setSelectAll(value: Boolean) = viewModelScope.launch {
        if (value) _selectedAlbumPojos.value = offsetAlbumPojos.first().filter {
            !_importedAlbumIds.value.contains(it.spotifyAlbum.id) && !_notFoundAlbumIds.value.contains(it.spotifyAlbum.id)
        }
        else _selectedAlbumPojos.value = emptyList()
    }

    fun toggleSelected(spotifyPojo: SpotifyAlbumPojo) {
        if (_selectedAlbumPojos.value.contains(spotifyPojo)) _selectedAlbumPojos.value -= spotifyPojo
        else if (
            !_importedAlbumIds.value.contains(spotifyPojo.spotifyAlbum.id) &&
            !_notFoundAlbumIds.value.contains(spotifyPojo.spotifyAlbum.id)
        ) _selectedAlbumPojos.value += spotifyPojo
    }


    /** PRIVATE FUNCTIONS *****************************************************/
    private fun fetchUserAlbumsIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        while (
            _filteredAlbumPojos.first().size < _localOffset.value + 100 &&
            !repos.spotify.allUserAlbumsFetched.value
        ) {
            _isSearching.value = true
            if (!repos.spotify.fetchNextUserAlbums()) break
        }
        _isSearching.value = false
    }

    private suspend fun findAlbumOnYoutube(
        spotifyPojo: SpotifyAlbumPojo,
        progressCallback: (Double) -> Unit = {},
    ): SpotifyAlbumMatch? {
        val album = Album(
            title = spotifyPojo.spotifyAlbum.name,
            isLocal = false,
            isInLibrary = true,
            artist = spotifyPojo.artist.takeIf { it.isNotEmpty() },
            year = spotifyPojo.spotifyAlbum.year,
        )
        val pojo = AlbumWithTracksPojo(
            album = album,
            genres = spotifyPojo.genres,
            tracks = spotifyPojo.spotifyTrackPojos.map {
                Track(
                    isInLibrary = true,
                    artist = it.artist,
                    albumId = album.albumId,
                    albumPosition = it.track.trackNumber,
                    title = it.track.name,
                    discNumber = it.track.discNumber,
                )
            },
            spotifyAlbum = spotifyPojo.spotifyAlbum.copy(albumId = album.albumId),
        )

        // First sort by Levenshtein distance, then put those with equal or
        // higher track count than pojo in front. #1 should be best match:
        return repos.youtube.getAlbumSearchResult(spotifyPojo.searchQuery, progressCallback)
            .asSequence()
            .map {
                if (it.tracks.size > spotifyPojo.spotifyTrackPojos.size)
                    it.copy(tracks = it.tracks.subList(0, spotifyPojo.spotifyTrackPojos.size))
                else it
            }
            .map {
                SpotifyAlbumMatch(
                    spotifyAlbumPojo = spotifyPojo.copy(
                        spotifyAlbum = spotifyPojo.spotifyAlbum.copy(albumId = album.albumId),
                    ),
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
