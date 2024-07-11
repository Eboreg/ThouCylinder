package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.slice
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.toUiStates
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState
import us.huseli.thoucylinder.dataclasses.artist.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.spotify.AbstractSpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumType
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySimplifiedAlbum
import us.huseli.thoucylinder.dataclasses.spotify.toNativeArtists
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.dataclasses.track.toUiStates
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
    private val managers: Managers,
) : AbstractAlbumListViewModel<AlbumUiState>("ArtistViewModel", repos, managers) {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val _isLoadingAlbums = MutableStateFlow(true)
    private val _isLoadingTracks = MutableStateFlow(true)
    private val _spotifyAlbumTypes =
        MutableStateFlow(listOf(SpotifyAlbumType.ALBUM, SpotifyAlbumType.SINGLE, SpotifyAlbumType.COMPILATION))
    private val artistId: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val artistCombo = repos.artist.flowArtistComboById(artistId)
    private val artistSpotifyId = artistCombo.map { it?.artist?.spotifyId }.distinctUntilChanged()

    override val baseAlbumUiStates: StateFlow<ImmutableList<AlbumUiState>> =
        repos.album.flowAlbumCombosByArtist(artistId)
            .map { it.toUiStates() }
            .onEach { _isLoadingAlbums.value = false }
            .stateLazily(persistentListOf())

    override val baseTrackUiStates: StateFlow<ImmutableList<TrackUiState>> =
        repos.track.flowTrackCombosByArtist(artistId)
            .map { it.toUiStates() }
            .onEach { _isLoadingTracks.value = false }
            .stateLazily(persistentListOf())

    val relatedArtists: StateFlow<ImmutableList<UnsavedArtist>> = artistSpotifyId
        .filterNotNull()
        .map { spotifyId ->
            repos.spotify.getRelatedArtists(spotifyId)?.toNativeArtists()?.toImmutableList() ?: persistentListOf()
        }
        .stateLazily(persistentListOf())

    private val _spotifyAlbums: Flow<List<SpotifySimplifiedAlbum>> = artistSpotifyId
        .filterNotNull()
        .map { spotifyId -> repos.spotify.flowArtistAlbums(spotifyId).toList() }

    val spotifyAlbums: StateFlow<ImmutableList<SpotifySimplifiedAlbum>> =
        combine(_spotifyAlbums, _spotifyAlbumTypes) { albums, albumTypes ->
            albums.filter { it.spotifyAlbumType in albumTypes }.toImmutableList()
        }.stateLazily(persistentListOf())

    val spotifyAlbumsPreview: StateFlow<ImmutableList<SpotifySimplifiedAlbum>> =
        combine(_spotifyAlbums, baseAlbumUiStates) { albums, uiStates ->
            albums.filter { it.id !in uiStates.map { state -> state.spotifyId } }
                .slice(0, 10)
                .toImmutableList()
        }.stateLazily(persistentListOf())

    val displayType = _displayType.asStateFlow()
    val isLoadingAlbums = _isLoadingAlbums.asStateFlow()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    val listType = _listType.asStateFlow()
    val spotifyAlbumTypes: StateFlow<ImmutableList<SpotifyAlbumType>> =
        _spotifyAlbumTypes.map { it.toImmutableList() }.stateLazily(persistentListOf())
    val uiState: StateFlow<ArtistUiState?> = artistCombo.map { it?.toUiState() }.stateLazily()

    fun getAlbumDownloadUiStateFlow(albumId: String) =
        managers.library.getAlbumDownloadUiStateFlow(albumId).stateLazily()

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateLazily()

    fun onSpotifyAlbumClick(spotifyAlbum: AbstractSpotifyAlbum, onGotoAlbumClick: (String) -> Unit) =
        managers.library.addTemporarySpotifyAlbum(spotifyAlbum, onGotoAlbumClick)

    fun onRelatedArtistClick(artist: UnsavedArtist, onGotoArtistClick: (String) -> Unit) {
        launchOnIOThread {
            val savedArtist = repos.artist.upsertArtist(artist)

            onMainThread { onGotoArtistClick(savedArtist.artistId) }
        }
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }

    fun toggleSpotifyAlbumType(albumType: SpotifyAlbumType) {
        if (albumType in _spotifyAlbumTypes.value) _spotifyAlbumTypes.value -= albumType
        else _spotifyAlbumTypes.value += albumType
    }
}
