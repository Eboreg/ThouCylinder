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
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.album.AlbumUiState
import us.huseli.thoucylinder.dataclasses.album.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.album.TrackMergeStrategy
import us.huseli.thoucylinder.dataclasses.artist.Artist
import us.huseli.thoucylinder.dataclasses.musicbrainz.MusicBrainzReleaseGroupBrowse
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.dataclasses.track.AlbumTrackUiState
import us.huseli.thoucylinder.dataclasses.track.TrackCombo
import us.huseli.thoucylinder.enums.AlbumType
import us.huseli.thoucylinder.enums.ListUpdateStrategy
import us.huseli.thoucylinder.interfaces.IExternalAlbum
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
    savedStateHandle: SavedStateHandle,
) : AbstractTrackListViewModel<AlbumTrackUiState>("AlbumViewModel", repos, managers) {
    data class MusicBrainzArtistAssociation(val native: Artist, val order: Int, val musicBrainzId: String)

    data class OtherArtistAlbums(
        val albumTypes: ImmutableList<AlbumType>,
        val albums: ImmutableList<IExternalAlbum>,
        val artist: Artist,
        val isExpanded: Boolean = false,
        val order: Int,
        val preview: ImmutableList<IExternalAlbum>,
        val musicBrainzArtistId: String,
    )

    private val _albumId = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val _albumNotFound = MutableStateFlow(false)
    private val _artists = repos.artist.flowArtistsByAlbumId(_albumId)
    private val _importProgress = MutableStateFlow(ProgressData())
    private val _otherArtistAlbumsAlbumTypes = MutableStateFlow<Map<String, List<AlbumType>>>(emptyMap())
    private val _otherArtistAlbumsExpanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _albumCombo: StateFlow<AlbumWithTracksCombo?> = repos.album.flowAlbumWithTracks(_albumId)
        .onEach { _albumNotFound.value = it == null }
        .filterNotNull()
        .stateWhileSubscribed()

    private val _primaryMusicBrainzArtists = combine(_albumCombo.filterNotNull(), _artists) { combo, artists ->
        if (combo.album.albumType != AlbumType.COMPILATION) {
            artists
                .associate { it.artist to it.artist.musicBrainzId }
                .filterValuesNotNull()
                .filterValues { it.isNotEmpty() }
                .toList()
                .mapIndexed { index, (artist, musicBrainzId) ->
                    MusicBrainzArtistAssociation(
                        native = artist,
                        musicBrainzId = musicBrainzId,
                        order = index,
                    )
                }
        } else emptyList()
    }

    private val _otherArtistAlbumsMap = _primaryMusicBrainzArtists.map { artists ->
        val result = mutableMapOf<MusicBrainzArtistAssociation, List<IExternalAlbum>>()

        for (artist in artists) {
            if (result.size < 3) {
                repos.musicBrainz.flowArtistReleaseGroups(artistId = artist.musicBrainzId).toList()
                    .sortedWith(MusicBrainzReleaseGroupBrowse.ReleaseGroup.comparator)
                    .takeIf { it.isNotEmpty() }
                    ?.also { result[artist] = it }
            }
        }
        result
    }

    private val _trackCombos: StateFlow<List<TrackCombo>> =
        repos.track.flowTrackCombosByAlbumId(_albumId).stateWhileSubscribed(emptyList())

    val otherArtistAlbums: StateFlow<ImmutableList<OtherArtistAlbums>> = combine(
        _otherArtistAlbumsMap,
        _otherArtistAlbumsAlbumTypes,
        _otherArtistAlbumsExpanded,
    ) { albumsMap, albumTypes, expanded ->
        albumsMap.map { (artist, artistAlbums) ->
            val artistAlbumTypes = albumTypes[artist.musicBrainzId] ?: AlbumType.entries
            val filtered = artistAlbums
                .filter { artistAlbumTypes.contains(it.albumType) }
                .filter { it.id != _albumCombo.value?.album?.musicBrainzReleaseGroupId }
                .toImmutableList()
            val filteredAlbums = filtered.filter { it.albumType == AlbumType.ALBUM }.toImmutableList()

            OtherArtistAlbums(
                albumTypes = artistAlbumTypes.toImmutableList(),
                albums = filtered,
                artist = artist.native,
                isExpanded = expanded[artist.musicBrainzId] ?: false,
                order = artist.order,
                preview = if (filteredAlbums.size >= 10) filteredAlbums else filtered,
                musicBrainzArtistId = artist.musicBrainzId,
            )
        }.sortedBy { it.order }.toImmutableList()
    }.stateWhileSubscribed(persistentListOf())

    val albumNotFound = _albumNotFound.asStateFlow()
    val importProgress: StateFlow<ProgressData> = _importProgress.asStateFlow()
    val downloadState: StateFlow<AlbumDownloadTask.UiState?> =
        managers.library.getAlbumDownloadUiStateFlow(_albumId).stateWhileSubscribed()

    val tagNames: StateFlow<ImmutableList<String>> = repos.album.flowTagsByAlbumId(_albumId)
        .map { tags -> tags.map { it.name }.toImmutableList() }
        .stateWhileSubscribed(persistentListOf())

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
        }.stateWhileSubscribed(persistentListOf())

    val positionColumnWidthDp: StateFlow<Int> = baseTrackUiStates.map { states ->
        val trackPositions = states.map { it.positionString }
        trackPositions.maxOfOrNull { it.length * 10 }?.plus(10) ?: 40
    }.stateWhileSubscribed(40)

    val uiState: StateFlow<AlbumUiState?> = _albumCombo.map { it?.toUiState() }.stateWhileSubscribed()

    val musicBrainzReleases = uiState.filterNotNull().map { state ->
        state.musicBrainzReleaseGroupId?.let { groupId ->
            repos.musicBrainz.listReleasesByReleaseGroupId(groupId).sortedBy { it.date }.toImmutableList()
        } ?: persistentListOf()
    }.stateWhileSubscribed(persistentListOf())

    init {
        unselectAllTracks()
        refetchIfNeeded()
    }

    override fun playTrack(state: AbstractTrackUiState) {
        val trackIdx = _trackCombos.value.indexOfFirst { it.track.trackId == state.trackId }

        if (trackIdx > -1) managers.player.playAlbum(_albumId, trackIdx)
    }

    override fun setTrackStateIsSelected(state: AlbumTrackUiState, isSelected: Boolean) =
        state.copy(isSelected = isSelected)

    fun ensureTrackMetadataAsync(trackId: String) = managers.library.ensureTrackMetadataAsync(trackId)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun matchUnplayableTracks() {
        launchOnMainThread {
            _albumCombo.value?.also { combo ->
                managers.library.matchUnplayableTracks(combo).collect { _importProgress.value = it }
            }
        }
    }

    fun onOtherArtistAlbumClick(externalId: String, onGotoAlbumClick: (String) -> Unit) =
        managers.library.addTemporaryMusicBrainzAlbum(externalId, onGotoAlbumClick)

    fun toggleOtherArtistAlbumsExpanded(obj: OtherArtistAlbums) {
        _otherArtistAlbumsExpanded.value += obj.musicBrainzArtistId to !obj.isExpanded
    }

    fun toggleOtherArtistAlbumsAlbumType(obj: OtherArtistAlbums, albumType: AlbumType) {
        val current = obj.albumTypes.toMutableList()

        if (current.contains(albumType)) current -= albumType
        else current += albumType
        _otherArtistAlbumsAlbumTypes.value += obj.musicBrainzArtistId to current
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
