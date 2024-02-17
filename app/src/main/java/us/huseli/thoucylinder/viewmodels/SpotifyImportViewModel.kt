package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.SpotifyOAuth2
import us.huseli.thoucylinder.dataclasses.ImportProgressData
import us.huseli.thoucylinder.dataclasses.ImportProgressStatus
import us.huseli.thoucylinder.dataclasses.combos.YoutubePlaylistCombo
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyResponseAlbumItem
import us.huseli.thoucylinder.dataclasses.spotify.filterBySearchTerm
import us.huseli.thoucylinder.dataclasses.spotify.toMediaStoreImage
import us.huseli.thoucylinder.umlautify
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _localOffset = MutableStateFlow(0)
    private val _selectedSpotifyAlbums = MutableStateFlow<List<SpotifyResponseAlbumItem.Album>>(emptyList())
    private val _progress = MutableStateFlow<ImportProgressData?>(null)
    private val _notFoundAlbumIds = MutableStateFlow<List<String>>(emptyList())
    private val _searchTerm = MutableStateFlow("")
    private val _spotifyAlbums = combine(repos.spotify.userAlbums, _searchTerm) { albums, term ->
        albums.filterBySearchTerm(term).filter { !_pastImportedAlbumIds.contains(it.id) }
    }
    private val _pastImportedAlbumIds = mutableSetOf<String>()
    private val _isSearching = MutableStateFlow(false)
    private val _importedAlbumIds = MutableStateFlow<Map<String, UUID>>(emptyMap())

    val offsetSpotifyAlbums = combine(_spotifyAlbums, _localOffset) { albums, offset ->
        albums.subList(min(offset, max(albums.lastIndex, 0)), min(offset + 50, albums.size))
    }
    val localOffset: StateFlow<Int> = _localOffset.asStateFlow()
    val selectedSpotifyAlbums: StateFlow<List<SpotifyResponseAlbumItem.Album>> = _selectedSpotifyAlbums.asStateFlow()
    val isAllSelected: Flow<Boolean> = combine(offsetSpotifyAlbums, _selectedSpotifyAlbums) { userAlbums, selected ->
        userAlbums.isNotEmpty() && selected.map { it.id }.containsAll(userAlbums.map { it.id })
    }
    val authorizationStatus: Flow<SpotifyOAuth2.AuthorizationStatus> = repos.spotify.oauth2.authorizationStatus
    val progress: StateFlow<ImportProgressData?> = _progress.asStateFlow()
    val nextAlbumIdx = repos.spotify.nextUserAlbumIdx
    val notFoundAlbumIds: StateFlow<List<String>> = _notFoundAlbumIds.asStateFlow()
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()
    val filteredAlbumCount: Flow<Int?> = combine(
        _searchTerm,
        _spotifyAlbums,
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
    val importedAlbumIds = _importedAlbumIds.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _pastImportedAlbumIds.addAll(repos.spotify.listImportedAlbumIds())
        }
    }

    suspend fun getThumbnail(spotifyAlbum: SpotifyResponseAlbumItem.Album): ImageBitmap? =
        repos.spotify.thumbnailCache.getOrNull(spotifyAlbum)

    fun getAuthUrl() = repos.spotify.oauth2.getAuthUrl()

    fun handleIntent(intent: Intent, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        try {
            repos.spotify.oauth2.handleIntent(intent)
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "handleIntent: $e", e)
            SnackbarEngine.addError(context.getString(R.string.spotify_authorization_failed, e).umlautify())
        }
    }

    fun importSelectedAlbums(onFinish: (importedIds: List<UUID>, notFoundCount: Int) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val selectedAlbums = _selectedSpotifyAlbums.value
            var notFoundCount = 0
            val importedIds = mutableListOf<UUID>()

            selectedAlbums.forEachIndexed { index, spotifyAlbum ->
                val progressBaseline = index.toDouble() / selectedAlbums.size
                val progressMultiplier = 1.0 / selectedAlbums.size
                val importProgressData = ImportProgressData(
                    item = spotifyAlbum.name.umlautify(),
                    progress = progressBaseline,
                    status = ImportProgressStatus.MATCHING,
                )

                _progress.value = importProgressData

                val match = findAlbumOnYoutube(spotifyAlbum) { progress ->
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + (progress * progressMultiplier * 0.9)
                    )
                }

                if (match != null) {
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + (progressMultiplier * 0.9),
                        status = ImportProgressStatus.IMPORTING,
                    )

                    val spotifyAlbumArt = spotifyAlbum.images.toMediaStoreImage()
                    val albumArt =
                        (spotifyAlbumArt ?: match.playlistCombo.playlist.getMediaStoreImage())
                            ?.let { repos.localMedia.saveInternalAlbumArtFiles(it, match.albumCombo.album) }
                    val albumCombo = match.albumCombo.copy(
                        album = match.albumCombo.album.copy(albumArt = albumArt),
                    )

                    repos.album.insertAlbumCombo(albumCombo)
                    repos.track.insertTracks(albumCombo.tracks)
                    _progress.value = importProgressData.copy(
                        progress = progressBaseline + progressMultiplier,
                        status = ImportProgressStatus.IMPORTING,
                    )
                    _importedAlbumIds.value += spotifyAlbum.id to albumCombo.album.albumId
                    importedIds.add(albumCombo.album.albumId)
                } else {
                    notFoundCount++
                    _notFoundAlbumIds.value += spotifyAlbum.id
                }

                _selectedSpotifyAlbums.value -= spotifyAlbum
            }

            _progress.value = null
            if (selectedAlbums.isNotEmpty()) {
                onFinish(importedIds, notFoundCount)
            }
        }

    fun setOffset(offset: Int) {
        _localOffset.value = offset
        _selectedSpotifyAlbums.value = emptyList()
        fetchUserAlbumsIfNeeded()
    }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            setOffset(0)
        }
    }

    fun setSelectAll(value: Boolean) = viewModelScope.launch {
        if (value) _selectedSpotifyAlbums.value = offsetSpotifyAlbums.first().filter {
            !_importedAlbumIds.value.containsKey(it.id) &&
                !_notFoundAlbumIds.value.contains(it.id)
        }
        else _selectedSpotifyAlbums.value = emptyList()
    }

    fun toggleSelected(spotifyAlbum: SpotifyResponseAlbumItem.Album) {
        if (_selectedSpotifyAlbums.value.contains(spotifyAlbum)) _selectedSpotifyAlbums.value -= spotifyAlbum
        else if (
            !_importedAlbumIds.value.containsKey(spotifyAlbum.id) &&
            !_notFoundAlbumIds.value.contains(spotifyAlbum.id)
        ) _selectedSpotifyAlbums.value += spotifyAlbum
    }


    /** PRIVATE FUNCTIONS *****************************************************/
    private fun fetchUserAlbumsIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        while (
            _spotifyAlbums.first().size < _localOffset.value + 100 &&
            !repos.spotify.allUserAlbumsFetched.value
        ) {
            _isSearching.value = true
            if (!repos.spotify.fetchNextUserAlbums()) break
        }
        _isSearching.value = false
    }

    private suspend fun findAlbumOnYoutube(
        responseAlbum: SpotifyResponseAlbumItem.Album,
        progressCallback: (Double) -> Unit = {},
    ): YoutubePlaylistCombo.AlbumMatch? =
        repos.youtube.matchAlbumWithTracks(responseAlbum.toAlbumCombo(isInLibrary = true), progressCallback)
            .filter { it.score < 1.0 }
            .minByOrNull { it.score }
}
