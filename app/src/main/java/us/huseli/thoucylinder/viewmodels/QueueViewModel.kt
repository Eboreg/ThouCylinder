package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import us.huseli.thoucylinder.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.radio.RadioUiState
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.track.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.dataclasses.track.toUiStates
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractTrackListViewModel<TrackUiState>("QueueViewModel", repos, managers) {
    private val _isLoading = MutableStateFlow(true)
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())

    private val areAllTracksSelected: Boolean
        get() = selectedTrackStateIds.value.containsAll(baseTrackUiStates.value.map { it.id })

    override val baseTrackUiStates = _queue.map { it.toUiStates() }.stateWhileSubscribed(persistentListOf())

    val isLoading = _isLoading.asStateFlow()
    val currentComboId: StateFlow<String?> =
        repos.player.currentCombo.map { it?.queueTrackId }.distinctUntilChanged().stateWhileSubscribed()
    val currentComboIndex: StateFlow<Int?> = combine(repos.player.queue, repos.player.currentCombo) { queue, combo ->
        queue.indexOf(combo).takeIf { it > -1 }
    }.stateWhileSubscribed()
    val radioUiState: StateFlow<RadioUiState?> =
        managers.radio.radioUiState.distinctUntilChanged().stateWhileSubscribed()

    init {
        launchOnIOThread {
            repos.player.queue.collect { queue ->
                _queue.value = queue
                _isLoading.value = false
            }
        }
    }

    override fun enqueueSelectedTracks() {
        launchOnMainThread { managers.player.moveTracksNext(queueTrackIds = getSortedSelectedTrackStates().map { it.id }) }
    }

    override fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks): TrackSelectionCallbacks {
        /** It makes little sense to define onPlayClick and onEnqueueClick here. */
        return super.getTrackSelectionCallbacks(dialogCallbacks).copy(
            onEnqueueClick = null,
            onPlayClick = null,
        )
    }

    override fun playSelectedTracks() {
        launchOnMainThread { repos.player.moveNextAndPlay(queueTrackIds = getSortedSelectedTrackStates().map { it.id }) }
    }

    override fun playTrack(state: AbstractTrackUiState) {
        val index = _queue.value.indexOfFirst { it.queueTrackId == state.id }

        if (index > -1) repos.player.skipTo(index)
    }

    override fun setTrackStateIsSelected(state: TrackUiState, isSelected: Boolean) = state.copy(isSelected = isSelected)

    fun clearQueue() = repos.player.clearQueue()

    fun deactivateRadio() = managers.radio.deactivateRadio()

    fun enqueueTrack(queueTrackId: String) {
        managers.player.moveTracksNext(listOf(queueTrackId))
    }

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun onMoveTrack(from: Int, to: Int) {
        /**
         * Only does visual move while reordering, does not store anything. Call onMoveTrackFinished() when reorder
         * operation is finished.
         */
        _queue.value = _queue.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = repos.player.onMoveTrackFinished(from, to)

    fun removeFromQueue(queueTrackId: String) {
        repos.player.removeFromQueue(listOf(queueTrackId))
        repos.track.unselectTracks("QueueViewModel", listOf(queueTrackId))
    }

    fun removeSelectedTracksFromQueue() {
        if (areAllTracksSelected) repos.player.clearQueue()
        else repos.player.removeFromQueue(queueTrackIds = selectedTrackStateIds.value)
        unselectAllTracks()
    }
}
