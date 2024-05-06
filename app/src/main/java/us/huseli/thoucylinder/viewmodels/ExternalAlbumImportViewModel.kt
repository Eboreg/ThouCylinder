package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.thoucylinder.PlaylistSearch
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.YoutubeWebClient
import us.huseli.thoucylinder.compose.screens.ImportBackend
import us.huseli.thoucylinder.dataclasses.LocalImportableAlbum
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.lastFm.LastFmTopAlbumsResponse
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.dataclasses.youtube.YoutubePlaylist
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.interfaces.filterBySearchTerm
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class ExternalAlbumImportViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _currentBackend = MutableStateFlow(ImportBackend.SPOTIFY)
    private val _displayOffset = MutableStateFlow(0)
    private val _importedAlbumIds = MutableStateFlow<ImmutableMap<String, String>>(persistentMapOf())
    private val _isSearching = MutableStateFlow(false)
    private val _localImportableAlbums = MutableStateFlow<List<LocalImportableAlbum>>(emptyList())
    private val _localImportUri = MutableStateFlow<Uri?>(null)
    private val _notFoundAlbumIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _progress = MutableStateFlow(ProgressData())
    private val _searchTerm = MutableStateFlow("")
    private val _selectedExternalAlbumIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _youtubeWebClient: StateFlow<YoutubeWebClient> =
        repos.youtube.region
            .map { YoutubeWebClient(region = it) }
            .stateEagerly(YoutubeWebClient(region = repos.youtube.region.value))
    private val _youtubePlaylists = MutableStateFlow<List<YoutubePlaylist>>(emptyList())
    private val _youtubePlaylistSearch = MutableStateFlow<PlaylistSearch?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _youtubePlaylistSearchHasMore = _youtubePlaylistSearch.flatMapLatest { it?.hasMore ?: flowOf(true) }

    private val _pastImportedAlbumIds: StateFlow<List<String>> = _currentBackend.map { backend ->
        when (backend) {
            ImportBackend.SPOTIFY -> repos.spotify.importedAlbumIds.value
            ImportBackend.LAST_FM -> repos.lastFm.importedReleaseIds.value
            ImportBackend.YOUTUBE -> emptyList()
            ImportBackend.LOCAL -> repos.youtube.importedPlaylistIds.value
        }
    }.distinctUntilChanged().stateLazily(emptyList())

    private val _externalAlbums: Flow<List<IExternalAlbum>> = combine(
        _currentBackend,
        repos.spotify.userAlbums,
        repos.lastFm.topAlbums,
        _localImportableAlbums,
        _youtubePlaylists,
    ) { backend, spotifyAlbums, lastFmAlbums, localAlbums, youtubePlaylists ->
        when (backend) {
            ImportBackend.SPOTIFY -> spotifyAlbums
            ImportBackend.LAST_FM -> lastFmAlbums
            ImportBackend.LOCAL -> localAlbums
            ImportBackend.YOUTUBE -> youtubePlaylists
        }
    }

    private val _filteredExternalAlbums: StateFlow<ImmutableList<IExternalAlbum>> = combine(
        _currentBackend,
        _externalAlbums,
        _searchTerm,
        _pastImportedAlbumIds,
    ) { backend, albums, term, pastIds ->
        val filteredAlbums = if (backend != ImportBackend.YOUTUBE) albums.filterBySearchTerm(term) else albums

        filteredAlbums.filter { !pastIds.contains(it.id) }.toImmutableList()
    }.distinctUntilChanged().stateLazily(persistentListOf())


    /** PUBLIC FLOWS **************************************************************************************************/

    val currentBackend = _currentBackend.asStateFlow()
    val displayOffset: StateFlow<Int> = _displayOffset.asStateFlow()
    val hasPrevious = _displayOffset.map { it > 0 }.distinctUntilChanged().stateLazily(false)
    val importedAlbumIds: StateFlow<ImmutableMap<String, String>> = _importedAlbumIds.asStateFlow()
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    val notFoundAlbumIds: StateFlow<ImmutableList<String>> = _notFoundAlbumIds.asStateFlow()
    val progress: StateFlow<ProgressData> = _progress.asStateFlow()
    val searchTerm: StateFlow<String> = _searchTerm.asStateFlow()
    val selectedExternalAlbumIds: StateFlow<ImmutableList<String>> = _selectedExternalAlbumIds.asStateFlow()

    val canImport: StateFlow<Boolean> = combine(
        _currentBackend,
        repos.spotify.oauth2PKCE.isAuthorized,
        repos.lastFm.username,
        _localImportUri,
    ) { backend, spotifyIsAuthorized, lastFmUsername, localUri ->
        when (backend) {
            ImportBackend.SPOTIFY -> spotifyIsAuthorized
            ImportBackend.LAST_FM -> lastFmUsername != null
            ImportBackend.LOCAL -> localUri != null
            ImportBackend.YOUTUBE -> true
        }
    }.distinctUntilChanged().stateLazily(false)

    val filteredAlbumCount: StateFlow<Int?> = combine(
        _filteredExternalAlbums,
        _currentBackend,
        _searchTerm,
        repos.spotify.totalUserAlbumCount,
    ) { albums, backend, term, spotifyTotal ->
        when (backend) {
            ImportBackend.SPOTIFY -> {
                if (term == "") spotifyTotal?.minus(_pastImportedAlbumIds.value.size)
                else albums.size
            }
            else -> albums.size
        }
    }.distinctUntilChanged().stateLazily(0)

    val isImportButtonEnabled = combine(_progress, _selectedExternalAlbumIds) { progress, albumIds ->
        !progress.isActive && albumIds.isNotEmpty()
    }.distinctUntilChanged().stateLazily(false)

    val isTotalAlbumCountExact: StateFlow<Boolean> = combine(
        _currentBackend,
        _searchTerm,
        repos.spotify.allUserAlbumsFetched,
        _youtubePlaylistSearchHasMore,
    ) { backend, term, spotifyAllFetched, youtubeHasMore ->
        when (backend) {
            ImportBackend.SPOTIFY -> term == "" || spotifyAllFetched
            ImportBackend.LAST_FM -> false
            ImportBackend.LOCAL -> true
            ImportBackend.YOUTUBE -> !youtubeHasMore
        }
    }.distinctUntilChanged().stateLazily(true)

    val offsetExternalAlbums: StateFlow<ImmutableList<IExternalAlbum>> =
        combine(_filteredExternalAlbums, _displayOffset) { albums, offset ->
            albums.subList(min(offset, max(albums.lastIndex, 0)), min(offset + 50, albums.size)).toImmutableList()
        }.distinctUntilChanged().stateLazily(persistentListOf())


    /** PUBLIC FLOWS DEPENDING ON OTHER PUBLIC FLOWS ******************************************************************/

    val currentAlbumCount: StateFlow<Int> = offsetExternalAlbums.map { it.size }
        .distinctUntilChanged()
        .stateLazily(0)

    val hasNext: StateFlow<Boolean> = combine(
        _currentBackend,
        _displayOffset,
        filteredAlbumCount,
        repos.lastFm.allTopAlbumsFetched,
    ) { backend, offset, albumCount, lastFmAllFetched ->
        albumCount == null || when (backend) {
            ImportBackend.LAST_FM -> (!lastFmAllFetched && albumCount > 0) || albumCount > offset + 50
            else -> albumCount > offset + 50
        }
    }.distinctUntilChanged().stateLazily(false)

    val isSelectAllEnabled: StateFlow<Boolean> = offsetExternalAlbums.map { it.isNotEmpty() }
        .distinctUntilChanged()
        .stateLazily(false)

    val isAllSelected: StateFlow<Boolean> =
        combine(offsetExternalAlbums, _selectedExternalAlbumIds) { albums, selectedIds ->
            albums.isNotEmpty() && selectedIds.containsAll(albums.map { it.id })
        }.distinctUntilChanged().stateLazily(false)


    /** PUBLIC METHODS ************************************************************************************************/

    init {
        launchOnMainThread {
            combineTransform(_currentBackend, canImport) { _, canImport -> if (canImport) emit(true) }
                .collect {
                    setOffset(0)
                    fetchExternalAlbumsIfNeeded()
                }
        }

        launchOnMainThread {
            _displayOffset.collect { fetchExternalAlbumsIfNeeded() }
        }
    }

    fun getSpotifyAuthUrl() = repos.spotify.oauth2PKCE.getAuthUrl()

    fun importSelectedAlbums(
        matchYoutube: Boolean,
        context: Context,
        onFinish: (List<AlbumUiState>, List<String>) -> Unit,
    ) {
        launchOnIOThread {
            val importedAlbumStates = mutableListOf<AlbumUiState>()
            val notFoundAlbums = mutableListOf<String>()
            val selectedExternalAlbums = _selectedExternalAlbumIds.value.mapNotNull { getExternalAlbum(it) }

            selectedExternalAlbums.forEachIndexed { index, externalAlbum ->
                val progressBaseline = index.toDouble() / selectedExternalAlbums.size
                val progressMultiplier = 1.0 / selectedExternalAlbums.size

                updateProgress(
                    text = context.getString(
                        if (matchYoutube) R.string.matching_x else R.string.importing_x,
                        externalAlbum.title,
                    ),
                )

                val combo = convertExternalAlbum(
                    externalAlbum = externalAlbum,
                    matchYoutube = matchYoutube,
                ) { progress -> updateProgress(progress = progressBaseline + (progress * progressMultiplier * 0.8)) }

                if (combo != null) {
                    withContext(Dispatchers.Main) {
                        updateProgress(
                            text = context.getString(R.string.importing_x, combo.album.title),
                            progress = progressBaseline + (progressMultiplier * 0.9),
                        )
                    }
                    managers.library.upsertAlbumCombo(combo)
                    _importedAlbumIds.value =
                        _importedAlbumIds.value.plus(externalAlbum.id to combo.album.albumId).toImmutableMap()
                    importedAlbumStates.add(AlbumUiState.fromAlbumCombo(combo))
                } else {
                    notFoundAlbums.add(
                        externalAlbum.artistName?.takeIf { it.isNotEmpty() }?.let { "$it - ${externalAlbum.title}" }
                            ?: externalAlbum.title,
                    )
                    _notFoundAlbumIds.value = _notFoundAlbumIds.value.plus(externalAlbum.id).toImmutableList()
                }

                _selectedExternalAlbumIds.value =
                    _selectedExternalAlbumIds.value.minus(externalAlbum.id).toImmutableList()
            }

            _progress.value = ProgressData()
            onFinish(importedAlbumStates, notFoundAlbums)
        }
    }

    fun isSelected(albumId: String) = _selectedExternalAlbumIds.value.contains(albumId)

    fun selectFromLastSelected(toId: String, allIds: List<String>) {
        val ids = _selectedExternalAlbumIds.value.lastOrNull()
            ?.let { allIds.listItemsBetween(it, toId).plus(toId) }
            ?: listOf(toId)

        _selectedExternalAlbumIds.value = _selectedExternalAlbumIds.value.plus(ids).toImmutableList()
    }

    fun setBackend(value: ImportBackend) {
        _currentBackend.value = value
    }

    fun setLocalImportUri(uri: Uri) {
        if (uri != _localImportUri.value) {
            _localImportUri.value = uri
            _localImportableAlbums.value = emptyList()
            _isSearching.value = true
            launchOnIOThread {
                managers.library.flowImportableAlbums(uri)
                    .collect { album -> _localImportableAlbums.value += album }
                _isSearching.value = false
            }
        }
    }

    fun setOffset(offset: Int) {
        _displayOffset.value = offset
        _selectedExternalAlbumIds.value = persistentListOf()
    }

    fun setSearchTerm(value: String) {
        if (value != _searchTerm.value) {
            _searchTerm.value = value
            setOffset(0)
            if (_currentBackend.value == ImportBackend.YOUTUBE && value.isNotEmpty()) {
                _isSearching.value = true
                _youtubePlaylists.value = emptyList()
                _youtubePlaylistSearch.value = PlaylistSearch(query = value, client = _youtubeWebClient.value)
                    .also { launchOnIOThread { fetchExternalAlbumsIfNeeded() } }
            }
        }
    }

    fun toggleSelectAll() {
        if (isAllSelected.value) _selectedExternalAlbumIds.value = persistentListOf()
        else _selectedExternalAlbumIds.value = offsetExternalAlbums.value.map { it.id }.toImmutableList()
    }

    fun toggleSelected(albumId: String) {
        if (_selectedExternalAlbumIds.value.contains(albumId))
            _selectedExternalAlbumIds.value = _selectedExternalAlbumIds.value.minus(albumId).toImmutableList()
        else if (
            !_importedAlbumIds.value.containsKey(albumId) &&
            !_notFoundAlbumIds.value.contains(albumId)
        ) _selectedExternalAlbumIds.value = _selectedExternalAlbumIds.value.plus(albumId).toImmutableList()
    }

    fun unauthorizeSpotify() = repos.spotify.unauthorize()


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun convertExternalAlbum(
        externalAlbum: IExternalAlbum,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo? {
        val getArtist: suspend (UnsavedArtist) -> Artist = { repos.artist.artistCache.get(it) }

        return when (externalAlbum) {
            is LastFmTopAlbumsResponse.LastFmAlbum -> {
                repos.musicBrainz.getRelease(externalAlbum.id)?.let { release ->
                    progressCallback(0.5)
                    release.toAlbumWithTracks(
                        getArtist = getArtist,
                        albumArt = externalAlbum.getMediaStoreImage(),
                        isInLibrary = true,
                        isLocal = false,
                    )
                }
            }
            is YoutubePlaylist -> _youtubeWebClient.value.getPlaylistComboFromPlaylistId(
                playlistId = externalAlbum.id,
                artist = externalAlbum.artist,
            )?.toAlbumWithTracks(
                isLocal = false,
                isInLibrary = true,
                getArtist = getArtist,
            )
            else -> externalAlbum.toAlbumWithTracks(
                isLocal = externalAlbum is LocalImportableAlbum,
                isInLibrary = true,
                getArtist = getArtist,
            )
        }
    }

    private suspend fun convertExternalAlbum(
        externalAlbum: IExternalAlbum,
        matchYoutube: Boolean,
        progressCallback: (Double) -> Unit,
    ): AlbumWithTracksCombo? {
        val progressBaseline = if (matchYoutube) 0.3 else 0.0
        val matchedCombo = convertExternalAlbum(
            externalAlbum = externalAlbum,
        ) { progressCallback(it * progressBaseline) } ?: return null

        if (!matchYoutube) {
            managers.library.updateAlbumFromMusicBrainzAsync(matchedCombo, TrackMergeStrategy.KEEP_SELF)
            return matchedCombo
        }

        val youtubeMatch = repos.youtube.getBestAlbumMatch(
            combo = matchedCombo,
            progressCallback = { progressCallback(progressBaseline + it - (it * progressBaseline)) },
        ) ?: return null

        // If imported & converted album already exists, use that instead:
        repos.album.getAlbumWithTracksByPlaylistId(youtubeMatch.playlistCombo.playlist.id)?.let { combo ->
            combo.copy(
                album = combo.album.copy(isInLibrary = true),
                trackCombos = combo.trackCombos.map { trackCombo ->
                    trackCombo.copy(track = trackCombo.track.copy(isInLibrary = true))
                },
            )
        }?.also { return it }

        // If newly imported album, try matching if with Musicbrainz too:
        managers.library.updateAlbumFromMusicBrainzAsync(youtubeMatch.albumCombo, TrackMergeStrategy.KEEP_SELF)
        return youtubeMatch.albumCombo
    }

    private suspend fun fetchExternalAlbums(): Boolean {
        return withContext(Dispatchers.IO) {
            when (_currentBackend.value) {
                ImportBackend.SPOTIFY -> repos.spotify.fetchNextUserAlbums()
                ImportBackend.LAST_FM -> repos.lastFm.fetchNextTopAlbums()
                ImportBackend.LOCAL -> false
                ImportBackend.YOUTUBE -> fetchNextYoutubePlaylists()
            }
        }
    }

    private suspend fun fetchExternalAlbumsIfNeeded() {
        while (_filteredExternalAlbums.value.size <= _displayOffset.value + 100) {
            _isSearching.value = true
            if (!fetchExternalAlbums()) break
        }
        _isSearching.value = false
    }

    private suspend fun fetchNextYoutubePlaylists(): Boolean {
        return _youtubePlaylistSearch.value?.let {
            it.flowResults(50).collect { playlist ->
                _youtubePlaylists.value += playlist
            }
            it.hasMore.value
        } ?: false
    }

    private fun getExternalAlbum(id: String): IExternalAlbum? = when (_currentBackend.value) {
        ImportBackend.SPOTIFY -> repos.spotify.userAlbums.value.find { it.id == id }
        ImportBackend.LAST_FM -> repos.lastFm.topAlbums.value.find { it.id == id }
        ImportBackend.LOCAL -> _localImportableAlbums.value.find { it.id == id }
        ImportBackend.YOUTUBE -> _youtubePlaylists.value.find { it.id == id }
    }

    private fun updateProgress(text: String? = null, progress: Double? = null) {
        _progress.value = ProgressData(
            text = text ?: _progress.value.text,
            progress = progress ?: _progress.value.progress,
            isActive = true,
        )
    }
}
