package us.huseli.thoucylinder.viewmodels

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import kotlin.math.max
import kotlin.math.min

abstract class AbstractTrackListViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    val selectedTrackIds: StateFlow<List<String>> = repos.track.flowSelectedTrackIds(selectionKey)
    val latestSelectedTrackId: StateFlow<String?> = selectedTrackIds.map { it.lastOrNull() }.stateLazily()

    open fun enqueueSelectedTracks() {
        managers.player.enqueueTracks(selectedTrackIds.value)
    }

    open suspend fun listSelectedTrackCombos(): ImmutableList<AbstractTrackCombo> =
        repos.track.listTrackCombosById(selectedTrackIds.value).toImmutableList()

    open suspend fun listSelectedTracks(): ImmutableList<Track> =
        repos.track.listTracksById(selectedTrackIds.value).toImmutableList()

    open fun playSelectedTracks() {
        managers.player.playTracks(selectedTrackIds.value)
    }

    fun enqueueTrackCombo(combo: AbstractTrackCombo) = managers.player.enqueueTrackCombo(combo)

    fun enqueueTrackCombos(combos: Collection<AbstractTrackCombo>) = managers.player.enqueueTrackCombos(combos)

    fun enqueueTrack(state: TrackUiState) = managers.player.enqueueTrackUiState(state)

    open fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks) = TrackSelectionCallbacks(
        onAddToPlaylistClick = { appCallbacks.onAddTracksToPlaylistClick(selectedTrackIds.value) },
        onPlayClick = { playSelectedTracks() },
        onEnqueueClick = { enqueueSelectedTracks() },
        onUnselectAllClick = { unselectAllTracks() },
    )

    fun playPlaylist(playlistId: String, startTrackId: String? = null) =
        managers.player.playPlaylist(playlistId, startTrackId)

    fun playTrack(state: TrackUiState) = managers.player.playTrackUiState(state)

    fun playTrackCombo(combo: AbstractTrackCombo) = managers.player.playTrackCombo(combo)

    fun playTrackCombos(combos: Collection<AbstractTrackCombo>, startIndex: Int = 0) =
        managers.player.playTrackCombos(combos, startIndex)

    fun selectTracksBetweenIndices(fromIndex: Int?, toIndex: Int, getTrackIdAtIndex: (Int) -> String?) {
        val trackIds = if (fromIndex != null)
            (min(fromIndex, toIndex)..max(fromIndex, toIndex)).mapNotNull { getTrackIdAtIndex(it) }
        else getTrackIdAtIndex(toIndex)?.let { listOf(it) }

        if (trackIds != null) repos.track.selectTrackIds(selectionKey, trackIds)
    }

    fun selectTracksFromLastSelected(to: String, allTrackIds: List<String>) {
        val trackIds = selectedTrackIds.value.lastOrNull()
            ?.let { allTrackIds.listItemsBetween(it, to).plus(to) }
            ?: listOf(to)

        repos.track.selectTrackIds(selectionKey, trackIds)
    }

    fun toggleTrackSelected(trackId: String) = repos.track.toggleTrackIdSelected(selectionKey, trackId)

    fun unselectAllTracks() = repos.track.unselectAllTrackIds(selectionKey)

    fun unselectTracks(trackIds: Collection<String>) = repos.track.unselectTrackIds(selectionKey, trackIds)
}
