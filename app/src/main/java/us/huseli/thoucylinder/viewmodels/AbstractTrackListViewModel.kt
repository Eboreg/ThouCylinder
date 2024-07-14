package us.huseli.thoucylinder.viewmodels

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import us.huseli.thoucylinder.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.track.AbstractTrackUiState
import us.huseli.thoucylinder.dataclasses.track.TrackSelectionCallbacks
import us.huseli.thoucylinder.listItemsBetween
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.sortedLike

abstract class AbstractTrackListViewModel<T : AbstractTrackUiState>(
    private val selectionKey: String,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    abstract val baseTrackUiStates: StateFlow<ImmutableList<T>>
    protected open val selectedTrackStateIds: StateFlow<Collection<String>> =
        repos.track.flowSelectedTrackStateIds(selectionKey)

    private val selectedTrackStates: StateFlow<List<T>>
        get() = combine(baseTrackUiStates, selectedTrackStateIds) { states, selected ->
            states.filter { selected.contains(it.id) }
        }.stateWhileSubscribed(emptyList())

    val selectedTrackCount: StateFlow<Int>
        get() = selectedTrackStateIds.map { it.size }.distinctUntilChanged().stateWhileSubscribed(0)

    val trackUiStates: StateFlow<ImmutableList<T>>
        get() = combine(baseTrackUiStates, selectedTrackStateIds) { states, selected ->
            states
                .map { state -> setTrackStateIsSelected(state, selected.contains(state.id)) }
                .toImmutableList()
        }.stateWhileSubscribed(persistentListOf())

    abstract fun setTrackStateIsSelected(state: T, isSelected: Boolean): T

    open fun enqueueSelectedTracks() {
        launchOnMainThread { managers.player.enqueueTracks(getSortedSelectedTrackStates().map { it.trackId }) }
    }

    open fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) = TrackSelectionCallbacks(
        onAddToPlaylistClick = { withSelectedTrackIds(dialogCallbacks.onAddTracksToPlaylistClick) },
        onEnqueueClick = { enqueueSelectedTracks() },
        onExportClick = { withSelectedTrackIds(dialogCallbacks.onExportTracksClick) },
        onPlayClick = { playSelectedTracks() },
        onSelectAllClick = { repos.track.selectTracks(selectionKey, baseTrackUiStates.value.map { it.id }) },
        onUnselectAllClick = { unselectAllTracks() },
    )

    open fun isTrackSelectEnabled(): Boolean {
        return selectedTrackStateIds.value.isNotEmpty()
    }

    open fun onTrackLongClick(trackId: String) {
        selectTrackStatesFromLastSelected(to = trackId)
    }

    open fun playSelectedTracks() {
        launchOnMainThread { managers.player.playTracks(getSortedSelectedTrackStates().map { it.trackId }) }
    }

    open fun playTrack(state: AbstractTrackUiState) {
        if (state.isPlayable) managers.player.playTrack(state.trackId)
    }

    open fun toggleTrackSelected(trackId: String) {
        repos.track.toggleTrackSelected(selectionKey, trackId)
    }

    suspend fun getSortedSelectedTrackStates(): List<T> =
        selectedTrackStates.first().sortedLike(baseTrackUiStates.value, key = { it.id })

    fun onTrackClick(state: AbstractTrackUiState) {
        if (isTrackSelectEnabled()) toggleTrackSelected(state.id)
        else playTrack(state)
    }

    fun unselectAllTracks() = repos.track.unselectAllTracks(selectionKey)

    private fun selectTrackStatesFromLastSelected(to: String) {
        val stateIds = selectedTrackStateIds.value.lastOrNull()
            ?.let { stateId ->
                baseTrackUiStates.value
                    .map { it.id }
                    .listItemsBetween(item1 = stateId, item2 = to)
                    .plus(to)
            }
            ?: listOf(to)

        repos.track.selectTracks(selectionKey, stateIds)
    }

    private fun withSelectedTrackIds(callback: (List<String>) -> Unit) {
        launchOnIOThread { callback(getSortedSelectedTrackStates().map { it.trackId }) }
    }
}
