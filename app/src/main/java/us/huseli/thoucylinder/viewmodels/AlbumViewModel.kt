package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
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
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.uistates.AlbumTrackUiState
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.getAlbumUiStateFlow
import us.huseli.thoucylinder.getTrackUiStateFlow
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
    savedStateHandle: SavedStateHandle,
) : AbstractTrackListViewModel("AlbumViewModel", repos, managers) {
    private val _albumId = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val _albumNotFound = MutableStateFlow(false)
    private val _importProgress = MutableStateFlow(ProgressData())

    private val _albumCombo: StateFlow<AlbumWithTracksCombo?> = repos.album.flowAlbumWithTracks(_albumId)
        .onEach { _albumNotFound.value = it == null }
        .filterNotNull()
        .distinctUntilChanged()
        .stateEagerly()

    private val _trackCombos: StateFlow<ImmutableList<TrackCombo>> = repos.track.flowTrackCombosByAlbumId(_albumId)
        .map { it.toImmutableList() }
        .distinctUntilChanged()
        .stateEagerly(persistentListOf())

    val albumArt: StateFlow<ImageBitmap?> = _albumCombo
        .filterNotNull()
        .map { managers.image.getFullImageBitmap(it.album.albumArt?.fullUri) }
        .distinctUntilChanged()
        .stateLazily()

    val albumNotFound = _albumNotFound.asStateFlow()

    val importProgress = _importProgress.asStateFlow()

    val tagNames: StateFlow<ImmutableList<String>> = repos.album.flowTagsByAlbumId(_albumId)
        .map { tags -> tags.map { it.name }.toImmutableList() }
        .distinctUntilChanged()
        .stateLazily(persistentListOf())

    val trackUiStates: StateFlow<ImmutableList<AlbumTrackUiState>> = combine(
        _albumCombo,
        _trackCombos,
        managers.library.trackDownloadTasks,
    ) { albumCombo, trackCombos, downloadTasks ->
        trackCombos.map { combo ->
            AlbumTrackUiState(
                trackId = combo.track.trackId,
                positionString = combo.track.getPositionString(albumCombo?.discCount ?: 1),
                trackArtists = combo.artists.toImmutableList(),
                isPlayable = combo.track.isPlayable,
                title = combo.track.title,
                duration = combo.track.duration,
                isDownloadable = combo.track.isDownloadable,
                isInLibrary = combo.track.isInLibrary,
                youtubeWebUrl = combo.track.youtubeWebUrl,
                spotifyWebUrl = combo.track.spotifyWebUrl,
                downloadState = downloadTasks.getTrackUiStateFlow(combo.track.trackId),
            )
        }.toImmutableList()
    }.distinctUntilChanged().stateLazily(persistentListOf())

    val positionColumnWidthDp: StateFlow<Int> = trackUiStates.map { states ->
        val trackPositions = states.map { it.positionString }
        trackPositions.maxOfOrNull { it.length * 10 }?.plus(10) ?: 40
    }.distinctUntilChanged().stateLazily(40)

    val uiState: StateFlow<AlbumUiState?> = combine(_albumCombo, managers.library.albumDownloadTasks) { combo, tasks ->
        combo?.let { AlbumUiState.fromAlbumCombo(it).copy(downloadState = tasks.getAlbumUiStateFlow(it.album.albumId)) }
    }.distinctUntilChanged().stateLazily()

    init {
        unselectAllTracks()
    }

    fun enqueueAlbum(albumId: String) = managers.player.enqueueAlbums(listOf(albumId))

    fun ensureTrackMetadataAsync(trackId: String) = managers.library.ensureTrackMetadataAsync(trackId)

    fun matchUnplayableTracks(context: Context) {
        launchOnIOThread {
            _albumCombo.value?.also { combo ->
                val unplayableTrackIds = combo.trackCombos.filter { !it.track.isPlayable }.map { it.track.trackId }
                val progressData = ProgressData(text = context.getString(R.string.matching), isActive = true)

                _importProgress.value = progressData

                val match = repos.youtube.getBestAlbumMatch(combo) { progress ->
                    _importProgress.value = progressData.copy(progress = progress * 0.5)
                }
                val matchedCombo = match?.albumCombo
                val updatedTracks =
                    matchedCombo?.trackCombos?.map { it.track }?.filter { unplayableTrackIds.contains(it.trackId) }
                        ?: emptyList()

                if (updatedTracks.isNotEmpty()) {
                    _importProgress.value =
                        progressData.copy(progress = 0.5, text = context.getString(R.string.importing))
                    repos.track.updateTracks(updatedTracks.map {
                        managers.library.ensureTrackMetadata(
                            it,
                            commit = false
                        )
                    })
                    _importProgress.value =
                        progressData.copy(progress = 0.9, text = context.getString(R.string.importing))
                    // So youtubePlaylist gets saved:
                    matchedCombo?.album?.also { repos.album.updateAlbum(it) }
                    SnackbarEngine.addInfo(
                        context.resources.getQuantityString(
                            R.plurals.x_tracks_matched_and_updated,
                            updatedTracks.size,
                            updatedTracks.size,
                        ).umlautify()
                    )
                } else {
                    SnackbarEngine.addError(context.getString(R.string.no_tracks_could_be_matched).umlautify())
                }

                _importProgress.value = ProgressData()
            }
        }
    }

    fun playAlbum(albumId: String, startIndex: Int = 0) = managers.player.playAlbum(albumId, startIndex)

    override fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks): TrackSelectionCallbacks =
        super.getTrackSelectionCallbacks(appCallbacks).copy(
            onSelectAllClick = {
                repos.track.selectTrackIds(
                    selectionKey = "AlbumViewModel",
                    trackIds = _albumCombo.value?.trackCombos?.map { it.track.trackId } ?: emptyList(),
                )
            }
        )

    fun selectTracksFromLastSelected(to: String) =
        selectTracksFromLastSelected(to = to, allTrackIds = _trackCombos.value.map { it.track.trackId })
}
