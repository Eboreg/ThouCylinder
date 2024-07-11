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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import us.huseli.retaintheme.extensions.slice
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.spotify.AbstractSpotifyAlbum
import us.huseli.thoucylinder.dataclasses.spotify.SpotifyAlbumType
import us.huseli.thoucylinder.dataclasses.spotify.SpotifySimplifiedAlbum
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.dataclasses.track.AlbumTrackUiState
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.enums.ListUpdateStrategy
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
    savedStateHandle: SavedStateHandle,
) : AbstractTrackListViewModel<AlbumTrackUiState>("AlbumViewModel", repos, managers) {
    data class SpotifyArtist(val native: Artist, val order: Int, val spotifyId: String)

    data class SpotifyAlbums(
        val albumTypes: ImmutableList<SpotifyAlbumType>,
        val albums: ImmutableList<SpotifySimplifiedAlbum>,
        val artist: Artist,
        val isExpanded: Boolean = false,
        val order: Int,
        val preview: ImmutableList<SpotifySimplifiedAlbum>,
        val spotifyArtistId: String,
    )

    private val _albumId = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val _albumNotFound = MutableStateFlow(false)
    private val _artists = repos.artist.flowArtistsByAlbumId(_albumId)
    private val _importProgress = MutableStateFlow(ProgressData())

    private val _albumCombo: StateFlow<AlbumWithTracksCombo?> = repos.album.flowAlbumWithTracks(_albumId)
        .onEach { _albumNotFound.value = it == null }
        .filterNotNull()
        .stateLazily()

    private val _trackCombos: StateFlow<List<TrackCombo>> =
        repos.track.flowTrackCombosByAlbumId(_albumId).stateLazily(emptyList())

    private val _primarySpotifyArtists: StateFlow<ImmutableList<SpotifyArtist>> =
        combine(_albumCombo, _artists) { combo, artists ->
            if (combo != null && combo.album.albumType != AlbumType.COMPILATION) {
                artists
                    .associate { it.artist to it.artist.spotifyId }
                    .filterValuesNotNull()
                    .filterValues { it.isNotEmpty() }
                    .toList()
                    .mapIndexed { index, (artist, spotifyId) ->
                        SpotifyArtist(
                            native = artist,
                            spotifyId = spotifyId,
                            order = index,
                        )
                    }
                    // .slice(0, 5)
                    .toImmutableList()
            } else persistentListOf()
        }.stateLazily(persistentListOf())

    private val _spotifyAlbumsMap = _primarySpotifyArtists.map { artists ->
        val result = mutableMapOf<SpotifyArtist, List<SpotifySimplifiedAlbum>>()

        for (artist in artists) {
            if (result.size < 3) {
                repos.spotify.flowArtistAlbums(artistId = artist.spotifyId).toList()
                    .takeIf { it.isNotEmpty() }
                    ?.also { result[artist] = it }
            }
        }
        result
    }

    private val _spotifyAlbumTypes = MutableStateFlow<Map<String, List<SpotifyAlbumType>>>(emptyMap())
    private val _spotifyAlbumsExpanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    val relatedAlbums: StateFlow<ImmutableList<SpotifyAlbums>> =
        combine(_spotifyAlbumsMap, _spotifyAlbumTypes, _spotifyAlbumsExpanded) { albumsMap, albumTypes, expanded ->
            albumsMap.map { (artist, artistAlbums) ->
                val artistAlbumTypes = albumTypes[artist.spotifyId] ?: SpotifyAlbumType.entries
                val filteredAlbums = artistAlbums
                    .filter { artistAlbumTypes.contains(it.spotifyAlbumType) }
                    .filter { it.id != _albumCombo.value?.album?.spotifyId }

                SpotifyAlbums(
                    albumTypes = artistAlbumTypes.toImmutableList(),
                    albums = filteredAlbums.toImmutableList(),
                    artist = artist.native,
                    isExpanded = expanded[artist.spotifyId] ?: false,
                    order = artist.order,
                    preview = filteredAlbums.slice(0, 10).toImmutableList(),
                    spotifyArtistId = artist.spotifyId,
                )
            }.sortedBy { it.order }.toImmutableList()
        }.stateLazily(persistentListOf())

    val albumNotFound = _albumNotFound.asStateFlow()
    val importProgress: StateFlow<ProgressData> = _importProgress.asStateFlow()
    val downloadState: StateFlow<AlbumDownloadTask.UiState?> =
        managers.library.getAlbumDownloadUiStateFlow(_albumId).stateLazily()

    val tagNames: StateFlow<ImmutableList<String>> = repos.album.flowTagsByAlbumId(_albumId)
        .map { tags -> tags.map { it.name }.toImmutableList() }
        .stateLazily(persistentListOf())

    override val baseTrackUiStates: StateFlow<ImmutableList<AlbumTrackUiState>> =
        combine(_albumCombo, _trackCombos) { albumCombo, trackCombos ->
            trackCombos.map { combo ->
                AlbumTrackUiState(
                    albumId = combo.track.albumId,
                    albumTitle = albumCombo?.album?.title,
                    artists = combo.trackArtists
                        .map { AbstractTrackUiState.Artist.fromArtistCredit(it) }
                        .toImmutableList(),
                    artistString = combo.artistString,
                    durationMs = combo.track.durationMs,
                    id = combo.track.trackId,
                    isDownloadable = combo.track.isDownloadable,
                    isInLibrary = combo.track.isInLibrary,
                    isPlayable = combo.track.isPlayable,
                    isSelected = false,
                    musicBrainzReleaseGroupId = combo.album?.musicBrainzReleaseGroupId,
                    musicBrainzReleaseId = combo.album?.musicBrainzReleaseId,
                    positionString = combo.track.getPositionString(albumCombo?.discCount ?: 1),
                    spotifyId = combo.track.spotifyId,
                    spotifyWebUrl = combo.track.spotifyWebUrl,
                    title = combo.track.title,
                    youtubeWebUrl = combo.track.youtubeWebUrl,
                    fullImageUrl = combo.fullImageUrl,
                    thumbnailUrl = combo.thumbnailUrl,
                )
            }.toImmutableList()
        }.stateLazily(persistentListOf())

    val positionColumnWidthDp: StateFlow<Int> = baseTrackUiStates.map { states ->
        val trackPositions = states.map { it.positionString }
        trackPositions.maxOfOrNull { it.length * 10 }?.plus(10) ?: 40
    }.stateLazily(40)

    val uiState: StateFlow<AlbumUiState?> = _albumCombo.map { it?.toUiState() }.stateLazily()

    init {
        unselectAllTracks()
        refetchIfNeeded()
    }

    override fun playTrack(state: AbstractTrackUiState) {
        val trackIdx = _trackCombos.value.indexOfFirst { it.track.trackId == state.trackId }

        if (trackIdx > -1) managers.player.playAlbum(_albumId, trackIdx)
    }

    override fun setTrackStateIsSelected(state: AlbumTrackUiState, isSelected: Boolean) = state.copy(isSelected = isSelected)

    fun ensureTrackMetadataAsync(trackId: String) = managers.library.ensureTrackMetadataAsync(trackId)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateLazily()

    fun matchUnplayableTracks() {
        launchOnMainThread {
            _albumCombo.value?.also { combo ->
                managers.library.matchUnplayableTracks(combo).collect { _importProgress.value = it }
            }
        }
    }

    fun onSpotifyAlbumClick(spotifyAlbum: AbstractSpotifyAlbum, onGotoAlbumClick: (String) -> Unit) =
        managers.library.addTemporarySpotifyAlbum(spotifyAlbum, onGotoAlbumClick)

    fun toggleSpotifyAlbumsExpanded(obj: SpotifyAlbums) {
        _spotifyAlbumsExpanded.value += obj.spotifyArtistId to !obj.isExpanded
    }

    fun toggleSpotifyAlbumType(obj: SpotifyAlbums, albumType: SpotifyAlbumType) {
        val current = obj.albumTypes.toMutableList()

        if (current.contains(albumType)) current -= albumType
        else current += albumType
        _spotifyAlbumTypes.value += obj.spotifyArtistId to current
    }

    private fun refetchIfNeeded() {
        launchOnIOThread {
            _albumCombo.transformWhile {
                if (it != null) emit(it)
                it == null
            }.collect { combo ->
                if (combo.album.trackCount != null && combo.album.trackCount > combo.tracks.size) {
                    val newCombo = combo.album.spotifyId?.let { spotifyId ->
                        repos.spotify
                            .getAlbum(spotifyId)
                            ?.toAlbumWithTracks(
                                isLocal = combo.album.isLocal,
                                isInLibrary = combo.album.isInLibrary,
                                albumId = combo.album.albumId,
                            )
                    } ?: combo.album.musicBrainzReleaseId?.let { musicBrainzId ->
                        repos.musicBrainz
                            .getRelease(musicBrainzId)
                            ?.toAlbumWithTracks(
                                isInLibrary = combo.album.isInLibrary,
                                isLocal = combo.album.isLocal,
                                albumId = combo.album.albumId,
                            )
                    }

                    if (newCombo != null) {
                        managers.library.upsertAlbumWithTracks(
                            combo.updateWith(
                                other = newCombo,
                                trackMergeStrategy = TrackMergeStrategy.KEEP_MOST,
                                albumArtistUpdateStrategy = ListUpdateStrategy.MERGE,
                                trackArtistUpdateStrategy = ListUpdateStrategy.MERGE,
                                tagUpdateStrategy = ListUpdateStrategy.MERGE,
                            )
                        )
                    }
                }
            }
        }
    }
}
