package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.toUiStates
import us.huseli.thoucylinder.dataclasses.artist.ArtistUiState
import us.huseli.thoucylinder.dataclasses.artist.UnsavedArtist
import us.huseli.thoucylinder.dataclasses.musicbrainz.MusicBrainzReleaseGroupBrowse
import us.huseli.thoucylinder.dataclasses.spotify.toNativeArtists
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.dataclasses.track.toUiStates
import us.huseli.thoucylinder.enums.AlbumType
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
    private val _otherAlbumTypes =
        MutableStateFlow(listOf(AlbumType.ALBUM, AlbumType.SINGLE, AlbumType.EP, AlbumType.COMPILATION))
    private val artistId: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val artistCombo = repos.artist.flowArtistComboById(artistId)
    private val artistMusicBrainzId = artistCombo.map { it?.artist?.musicBrainzId }.distinctUntilChanged()
    private val artistSpotifyId = artistCombo.map { it?.artist?.spotifyId }.distinctUntilChanged()

    override val baseAlbumUiStates: StateFlow<ImmutableList<AlbumUiState>> =
        repos.album.flowAlbumCombosByArtist(artistId)
            .map { it.toUiStates() }
            .onEach { _isLoadingAlbums.value = false }
            .stateWhileSubscribed(persistentListOf())

    override val baseTrackUiStates: StateFlow<ImmutableList<TrackUiState>> =
        repos.track.flowTrackCombosByArtist(artistId)
            .map { it.toUiStates() }
            .onEach { _isLoadingTracks.value = false }
            .stateWhileSubscribed(persistentListOf())

    val relatedArtists: StateFlow<ImmutableList<UnsavedArtist>> = artistSpotifyId
        .filterNotNull()
        .map { spotifyId ->
            repos.spotify.getRelatedArtists(spotifyId)?.toNativeArtists()?.toImmutableList() ?: persistentListOf()
        }
        .stateWhileSubscribed(persistentListOf())

    private val _musicBrainzReleaseGroups = artistMusicBrainzId
        .filterNotNull()
        .map { mbid ->
            repos.musicBrainz.flowArtistReleaseGroups(mbid)
                .toList()
                .sortedWith(MusicBrainzReleaseGroupBrowse.ReleaseGroup.comparator)
        }

    val otherAlbums: StateFlow<ImmutableList<MusicBrainzReleaseGroupBrowse.ReleaseGroup>> =
        combine(_musicBrainzReleaseGroups, _otherAlbumTypes) { releaseGroups, albumTypes ->
            releaseGroups.filter { it.albumType in albumTypes }.toImmutableList()
        }.stateWhileSubscribed(persistentListOf())

    val otherAlbumsPreview =
        combine(_musicBrainzReleaseGroups, baseAlbumUiStates) { releaseGroups, uiStates ->
            releaseGroups
                .filter { it.id !in uiStates.map { state -> state.musicBrainzReleaseGroupId } }
                .let { groups ->
                    val albums = groups.filter { it.albumType == AlbumType.ALBUM }
                    if (albums.size >= 10) albums else groups
                }
                .toImmutableList()
        }.stateWhileSubscribed(persistentListOf())

    val displayType = _displayType.asStateFlow()
    val isLoadingAlbums = _isLoadingAlbums.asStateFlow()
    val isLoadingTracks = _isLoadingTracks.asStateFlow()
    val listType = _listType.asStateFlow()
    val otherAlbumTypes: StateFlow<ImmutableList<AlbumType>> =
        _otherAlbumTypes.map { it.toImmutableList() }.stateWhileSubscribed(persistentListOf())
    val uiState: StateFlow<ArtistUiState?> = artistCombo.map { it?.toUiState() }.stateWhileSubscribed()

    fun getAlbumDownloadUiStateFlow(albumId: String) =
        managers.library.getAlbumDownloadUiStateFlow(albumId).stateWhileSubscribed()

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun onOtherAlbumClick(releaseGroupId: String, onGotoAlbumClick: (String) -> Unit) =
        managers.library.addTemporaryMusicBrainzAlbum(releaseGroupId, onGotoAlbumClick)

    fun onRelatedArtistClick(artist: UnsavedArtist, onGotoArtistClick: (String) -> Unit) {
        launchOnIOThread {
            val savedArtist = repos.artist.upsertArtist(artist)

            onMainThread { onGotoArtistClick(savedArtist.artistId) }

            repos.musicBrainz.matchArtist(artist)?.also { musicBrainzArtist ->
                repos.artist.upsertArtist(savedArtist.copy(musicBrainzId = musicBrainzArtist.id))
            }
        }
    }

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }

    fun toggleOtherAlbumsType(albumType: AlbumType) {
        if (albumType in _otherAlbumTypes.value) _otherAlbumTypes.value -= albumType
        else _otherAlbumTypes.value += albumType
    }
}
