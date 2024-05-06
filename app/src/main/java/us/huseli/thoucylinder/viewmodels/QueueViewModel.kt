package us.huseli.thoucylinder.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.RadioPojo
import us.huseli.thoucylinder.dataclasses.uistates.ModalCoverTrackUiState
import us.huseli.thoucylinder.dataclasses.uistates.ModalCoverTrackUiStateLight
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.getTrackUiStateFlow
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import kotlin.time.DurationUnit

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractTrackListViewModel("QueueViewModel", repos, managers) {
    private val _currentFullImage: StateFlow<ImageBitmap?> = repos.player.currentCombo.map { combo ->
        combo?.let { managers.image.getTrackComboFullImageBitmap(it) }
    }.distinctUntilChanged().stateLazily()
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())
    private val _nextFullImage: StateFlow<ImageBitmap?> = repos.player.nextCombo.map { combo ->
        combo?.let { managers.image.getTrackComboFullImageBitmap(it) }
    }.distinctUntilChanged().stateLazily()
    private val _previousFullImage = repos.player.previousCombo.map { combo ->
        combo?.let { managers.image.getTrackComboFullImageBitmap(it) }
    }.distinctUntilChanged().stateLazily()

    private val areAllTracksSelected: Boolean
        get() = selectedTrackIds.value.containsAll(trackUiStates.value.map { it.id })

    val canGotoNext: StateFlow<Boolean> = repos.player.canGotoNext
    val canPlay: StateFlow<Boolean> = repos.player.canPlay.stateLazily(false)
    val currentComboId: StateFlow<String?> =
        repos.player.currentCombo.map { it?.queueTrackId }.distinctUntilChanged().stateLazily()
    val currentProgress: StateFlow<Float> =
        combine(repos.player.currentPositionMs, repos.player.currentCombo) { position, combo ->
            val endPosition = combo?.track?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }
            endPosition?.let { position / it.toFloat() } ?: 0f
        }.distinctUntilChanged().stateLazily(0f)
    val isLoading: StateFlow<Boolean> = repos.player.isLoading
    val isPlaying: StateFlow<Boolean> = repos.player.isPlaying.stateLazily(false)
    val isRepeatEnabled: StateFlow<Boolean> = repos.player.isRepeatEnabled
    val isShuffleEnabled: StateFlow<Boolean> = repos.player.isShuffleEnabled
    val radioPojo: StateFlow<RadioPojo?> = managers.radio.radioPojo.stateLazily()
    val trackUiStates: StateFlow<ImmutableList<TrackUiState>> =
        combine(_queue, managers.library.trackDownloadTasks) { queue, tasks ->
            queue.map { combo ->
                TrackUiState.fromQueueTrackCombo(combo)
                    .copy(downloadState = tasks.getTrackUiStateFlow(combo.track.trackId))
            }.toImmutableList()
        }.distinctUntilChanged().stateEagerly(persistentListOf())

    val modalCoverTrackUiState: StateFlow<ModalCoverTrackUiState?> = combine(
        repos.player.currentCombo,
        _currentFullImage,
    ) { combo, currentFullImage ->
        if (combo != null) {
            ModalCoverTrackUiState(
                trackId = combo.track.trackId,
                durationMs = combo.track.durationMs,
                isDownloadable = combo.track.isDownloadable,
                isInLibrary = combo.track.isInLibrary,
                artists = combo.artists.plus(combo.albumArtists).toImmutableSet(),
                youtubeWebUrl = combo.track.youtubeWebUrl,
                spotifyWebUrl = combo.track.spotifyWebUrl,
                title = combo.track.title,
                artistString = combo.artists.joined() ?: combo.albumArtists.joined(),
                isPlayable = combo.track.isPlayable,
                fullImage = currentFullImage,
            )
        } else null
    }.distinctUntilChanged().stateLazily()

    val modalCoverNextTrackUiState: StateFlow<ModalCoverTrackUiStateLight?> =
        combine(repos.player.nextCombo, _nextFullImage) { combo, fullImage ->
            if (combo != null) ModalCoverTrackUiStateLight(
                title = combo.track.title,
                trackArtistString = combo.artists.joined(),
                fullImage = fullImage,
            ) else null
        }.distinctUntilChanged().stateLazily()

    val modalCoverPreviousTrackUiState: StateFlow<ModalCoverTrackUiStateLight?> =
        combine(repos.player.previousCombo, _previousFullImage) { combo, fullImage ->
            if (combo != null) ModalCoverTrackUiStateLight(
                title = combo.track.title,
                trackArtistString = combo.artists.joined(),
                fullImage = fullImage,
            ) else null
        }.distinctUntilChanged().stateLazily()

    val currentAmplitudes: StateFlow<ImmutableList<Int>> = repos.player.currentCombo.flatMapConcat { combo ->
        flow {
            combo?.track?.amplitudeList?.also { emit(it) } ?: kotlin.run {
                emit(persistentListOf())
                combo?.track?.let { withContext(Dispatchers.IO) { repos.track.getAmplitudes(it) } }?.also { emit(it) }
            }
        }
    }.stateLazily(persistentListOf())

    init {
        launchOnIOThread {
            repos.player.queue.collect { queue -> _queue.value = queue }
        }
    }

    fun deactivateRadio() = managers.radio.deactivateRadio()

    suspend fun ensureTrackMetadata(uiState: TrackUiState) = managers.library.ensureTrackMetadata(uiState.trackId)

    suspend fun ensureTrackMetadata(track: Track) = managers.library.ensureTrackMetadata(track)

    suspend fun getTrackUiStateThumbnail(uiState: TrackUiState): ImageBitmap? =
        managers.image.getTrackUiStateThumbnailImageBitmap(uiState)

    fun onMoveTrack(from: Int, to: Int) {
        /**
         * Only does visual move while reordering, does not store anything. Call onMoveTrackFinished() when reorder
         * operation is finished.
         */
        _queue.value = _queue.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = repos.player.onMoveTrackFinished(from, to)

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun removeFromQueue(queueTrackId: String) {
        repos.player.removeFromQueue(listOf(queueTrackId))
        unselectTracks(listOf(queueTrackId))
    }

    fun removeSelectedTracksFromQueue() {
        if (areAllTracksSelected) repos.player.clearQueue()
        else repos.player.removeFromQueue(selectedTrackIds.value)
        unselectAllTracks()
    }

    fun seekToProgress(progress: Float) = repos.player.seekToProgress(progress)

    fun skipTo(index: Int) = repos.player.skipTo(index)

    fun skipToNext() = repos.player.skipToNext()

    fun skipToPrevious() = repos.player.skipToPrevious()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleShuffle() = repos.player.toggleShuffle()

    override fun enqueueSelectedTracks() {
        managers.player.moveTracksNext(selectedTrackIds.value)
    }

    override fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks): TrackSelectionCallbacks {
        /** It makes little sense to define onPlayClick and onEnqueueClick here. */
        return TrackSelectionCallbacks(
            onAddToPlaylistClick = { appCallbacks.onAddTracksToPlaylistClick(selectedTrackIds.value) },
            onUnselectAllClick = { unselectAllTracks() },
            onSelectAllClick = { repos.track.selectTrackIds("QueueViewModel", trackUiStates.value.map { it.id }) },
        )
    }

    override suspend fun listSelectedTrackCombos() =
        repos.player.listQueueTrackCombosById(selectedTrackIds.value).toImmutableList()

    override suspend fun listSelectedTracks(): ImmutableList<Track> =
        listSelectedTrackCombos().map { it.track }.toImmutableList()

    override fun playSelectedTracks() {
        launchOnMainThread { repos.player.moveNextAndPlay(selectedTrackIds.value) }
    }
}
