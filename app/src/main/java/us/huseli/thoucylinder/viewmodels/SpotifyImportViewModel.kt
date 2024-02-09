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
import us.huseli.thoucylinder.ContextMutexCache
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.dataclasses.ImportProgressStatus
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.SpotifyAlbumCombo
import us.huseli.thoucylinder.dataclasses.combos.filterBySearchTerm
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class SpotifyAlbumMatch(
    val albumWithTracksCombo: AlbumWithTracksCombo,
    val spotifyAlbumCombo: SpotifyAlbumCombo,
    val youtubeAlbumCombo: AlbumWithTracksCombo,
    val levenshtein: Double,
)

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _localOffset = MutableStateFlow(0)
    private val _selectedAlbumCombos = MutableStateFlow<List<SpotifyAlbumCombo>>(emptyList())
    private val _thumbnailCache = ContextMutexCache<SpotifyAlbum, String, ImageBitmap>(
        keyFromInstance = { spotifyAlbum -> spotifyAlbum.id },
        fetchMethod = { spotifyAlbum, context -> spotifyAlbum.getThumbnailImageBitmap(context) }
    )
    private val _progress = MutableStateFlow<ImportProgressData?>(null)
    private val _importedAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _searchTerm = MutableStateFlow("")
    private val _albumCombos = repos.spotify.userAlbumCombos.map { combos ->
        combos.filter { !_pastImportedAlbumIds.contains(it.spotifyAlbum.id) }
    }
    private val _filteredAlbumCombos: Flow<List<SpotifyAlbumCombo>> =
        combine(_albumCombos, _searchTerm) { combos, term -> combos.filterBySearchTerm(term) }
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _isSearching = MutableStateFlow(false)

    val offsetAlbumCombos: Flow<List<SpotifyAlbumCombo>> = combine(_filteredAlbumCombos, _localOffset) { combos, offset ->
        combos.subList(min(offset, max(combos.lastIndex, 0)), min(offset + 50, combos.size))
    }

    val localOffset: StateFlow<Int> = _localOffset.asStateFlow()
    val selectedAlbumCombos: StateFlow<List<SpotifyAlbumCombo>> = _selectedAlbumCombos.asStateFlow()
    val isAllSelected: Flow<Boolean> = combine(offsetAlbumCombos, _selectedAlbumCombos) { userAlbums, selected ->
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
        _filteredAlbumCombos,
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

    suspend fun getThumbnail(spotifyAlbum: SpotifyAlbum, context: Context): ImageBitmap? =
        _thumbnailCache.get(spotifyAlbum, context)

    fun importSelectedAlbums(onFinish: (importCount: Int, notFoundCount: Int) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val selectedCombos = _selectedAlbumCombos.value
            var notFoundCount = 0

            selectedCombos.forEachIndexed { index, spotifyCombo ->
                val progressBaseline = index.toDouble() / selectedCombos.size
                val progressMultiplier = 1.0 / selectedCombos.size
                val importProgressData = ImportProgressData(
                    item = spotifyCombo.spotifyAlbum.name,
                    progress = progressBaseline,
                    status = ImportProgressStatus.MATCHING,
                )

                _progress.value = importProgressData

                val match = findAlbumOnYoutube(spotifyCombo) { progress ->
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
                    val tracks = match.youtubeAlbumCombo.tracks.mapIndexed { trackIdx, trackYoutube ->
                        val spotifyTrackCombo = match.spotifyAlbumCombo.spotifyTrackCombos.getOrNull(trackIdx)
                        val (title, artist) =
                            if (spotifyTrackCombo?.hasSimilarDuration(trackYoutube) == true)
                                Pair(spotifyTrackCombo.track.name, spotifyTrackCombo.artist)
                            else Pair(trackYoutube.title, trackYoutube.artist)
                        trackYoutube.copy(title = title, artist = artist, isInLibrary = true)
                    }
                    val spotifyTrackCombos =
                        match.spotifyAlbumCombo.spotifyTrackCombos.mapIndexed { trackIdx, spotifyTrackCombo ->
                            tracks.getOrNull(trackIdx)
                                ?.let { spotifyTrackCombo.copy(track = spotifyTrackCombo.track.copy(trackId = it.trackId)) }
                                ?: spotifyTrackCombo
                        }
                    val imageUrl = match.spotifyAlbumCombo.spotifyAlbum.fullImage?.url
                        ?: match.youtubeAlbumCombo.album.youtubePlaylist?.fullImage?.url
                    val albumArt =
                        imageUrl?.let {
                            repos.localMedia.saveInternalAlbumArtFiles(
                                match.albumWithTracksCombo.album,
                                it
                            )
                        }
                    val albumCombo = match.youtubeAlbumCombo.copy(
                        album = match.albumWithTracksCombo.album.copy(
                            youtubePlaylist = match.youtubeAlbumCombo.album.youtubePlaylist,
                            albumArt = albumArt,
                        ),
                        tracks = tracks.map { it.copy(albumId = match.albumWithTracksCombo.album.albumId) },
                        spotifyAlbum = match.spotifyAlbumCombo.spotifyAlbum,
                    )
                    val spotifyAlbumCombo = match.spotifyAlbumCombo.copy(
                        spotifyTrackCombos = spotifyTrackCombos,
                    )

                    repos.album.saveAlbumCombo(albumCombo)
                    repos.track.insertTracks(albumCombo.tracks)
                    repos.spotify.saveSpotifyAlbumCombo(spotifyAlbumCombo)
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + progressMultiplier,
                        status = ImportProgressStatus.IMPORTING,
                    )
                    _importedAlbumIds.value += spotifyCombo.spotifyAlbum.id
                } else {
                    notFoundCount++
                    _notFoundAlbumIds.value += spotifyCombo.spotifyAlbum.id
                }

                _selectedAlbumCombos.value -= spotifyCombo
            }

            _progress.value = null
            if (selectedCombos.isNotEmpty()) {
                onFinish(selectedCombos.size - notFoundCount, notFoundCount)
            }
        }

    fun setAuthorizationResponse(value: AuthorizationResponse) {
        Log.i(javaClass.simpleName, "setAuthorizationResponse: type=${value.type}, error=${value.error}")
        repos.spotify.setAuthorizationResponse(value)
    }

    fun setOffset(offset: Int) {
        _localOffset.value = offset
        _selectedAlbumCombos.value = emptyList()
        fetchUserAlbumsIfNeeded()
    }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            setOffset(0)
        }
    }

    fun setSelectAll(value: Boolean) = viewModelScope.launch {
        if (value) _selectedAlbumCombos.value = offsetAlbumCombos.first().filter {
            !_importedAlbumIds.value.contains(it.spotifyAlbum.id) && !_notFoundAlbumIds.value.contains(it.spotifyAlbum.id)
        }
        else _selectedAlbumCombos.value = emptyList()
    }

    fun toggleSelected(spotifyCombo: SpotifyAlbumCombo) {
        if (_selectedAlbumCombos.value.contains(spotifyCombo)) _selectedAlbumCombos.value -= spotifyCombo
        else if (
            !_importedAlbumIds.value.contains(spotifyCombo.spotifyAlbum.id) &&
            !_notFoundAlbumIds.value.contains(spotifyCombo.spotifyAlbum.id)
        ) _selectedAlbumCombos.value += spotifyCombo
    }


    /** PRIVATE FUNCTIONS *****************************************************/
    private fun fetchUserAlbumsIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        while (
            _filteredAlbumCombos.first().size < _localOffset.value + 100 &&
            !repos.spotify.allUserAlbumsFetched.value
        ) {
            _isSearching.value = true
            if (!repos.spotify.fetchNextUserAlbums()) break
        }
        _isSearching.value = false
    }

    private suspend fun findAlbumOnYoutube(
        spotifyCombo: SpotifyAlbumCombo,
        progressCallback: (Double) -> Unit = {},
    ): SpotifyAlbumMatch? {
        val album = Album(
            title = spotifyCombo.spotifyAlbum.name,
            isLocal = false,
            isInLibrary = true,
            artist = spotifyCombo.artist.takeIf { it.isNotEmpty() },
            year = spotifyCombo.spotifyAlbum.year,
        )
        val combo = AlbumWithTracksCombo(
            album = album,
            genres = spotifyCombo.genres,
            tracks = spotifyCombo.spotifyTrackCombos.map {
                Track(
                    isInLibrary = true,
                    artist = it.artist,
                    albumId = album.albumId,
                    albumPosition = it.track.trackNumber,
                    title = it.track.name,
                    discNumber = it.track.discNumber,
                )
            },
            spotifyAlbum = spotifyCombo.spotifyAlbum.copy(albumId = album.albumId),
        )

        // First sort by Levenshtein distance, then put those with equal or
        // higher track count than combo in front. #1 should be best match:
        return repos.youtube.getAlbumSearchResult(spotifyCombo.searchQuery, progressCallback)
            .asSequence()
            .map {
                if (it.tracks.size > spotifyCombo.spotifyTrackCombos.size)
                    it.copy(tracks = it.tracks.subList(0, spotifyCombo.spotifyTrackCombos.size))
                else it
            }
            .map {
                SpotifyAlbumMatch(
                    spotifyAlbumCombo = spotifyCombo.copy(
                        spotifyAlbum = spotifyCombo.spotifyAlbum.copy(albumId = album.albumId),
                    ),
                    youtubeAlbumCombo = it,
                    levenshtein = combo.getLevenshteinDistance(it),
                    albumWithTracksCombo = combo,
                )
            }
            .filter { it.levenshtein < 10.0 }
            .sortedBy { it.levenshtein }
            .minByOrNull { it.youtubeAlbumCombo.tracks.size <= it.spotifyAlbumCombo.spotifyTrackCombos.size }
    }
}
