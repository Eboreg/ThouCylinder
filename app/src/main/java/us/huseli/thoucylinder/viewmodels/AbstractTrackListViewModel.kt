package us.huseli.thoucylinder.viewmodels

import android.content.Context
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.extensions.listItemsBetween
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.launchOnMainThread
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.umlautify
import kotlin.math.max
import kotlin.math.min

abstract class AbstractTrackListViewModel(
    private val selectionKey: String,
    private val repos: Repositories,
) : AbstractBaseViewModel(repos) {
    private val _trackDownloadStates = MutableStateFlow<ImmutableList<TrackDownloadTask.ViewState>>(persistentListOf())

    val selectedTrackIds: StateFlow<List<String>> = repos.track.flowSelectedTrackIds(selectionKey)
    val latestSelectedTrackId = selectedTrackIds.map { it.lastOrNull() }
    val trackDownloadStates = _trackDownloadStates.asStateFlow()

    init {
        launchOnIOThread {
            repos.download.tasks.collect { tasks ->
                tasks.forEach { task ->
                    task.viewState.filterNotNull().collect { state ->
                        _trackDownloadStates.value = _trackDownloadStates.value.toMutableList().run {
                            removeIf { it.trackId == state.trackId }
                            add(state)
                            toImmutableList()
                        }
                    }
                }
            }
        }
    }

    open fun enqueueSelectedTracks(context: Context) {
        launchOnIOThread { enqueueTrackCombos(listSelectedTrackCombos(), context) }
    }

    open suspend fun listSelectedTrackCombos(): ImmutableList<AbstractTrackCombo> =
        repos.track.listTrackCombosById(selectedTrackIds.value).toImmutableList()

    open suspend fun listSelectedTracks(): ImmutableList<Track> =
        repos.track.listTracksById(selectedTrackIds.value).toImmutableList()

    open fun playSelectedTracks() {
        launchOnIOThread { playTrackCombos(listSelectedTrackCombos()) }
    }

    fun enqueueTrackCombo(combo: AbstractTrackCombo, context: Context) = enqueueTrackCombos(listOf(combo), context)

    fun enqueueTrackCombos(combos: Collection<AbstractTrackCombo>, context: Context) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombos(combos)

        withContext(Dispatchers.Main) { repos.player.insertNext(queueTrackCombos) }
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, combos.size, combos.size).umlautify()
        )
    }

    fun enqueueTrack(state: Track.ViewState, context: Context) = enqueueTracks(listOf(state), context)

    fun enqueueTracks(states: Collection<Track.ViewState>, context: Context) = launchOnIOThread {
        val queueTrackCombos = getQueueTrackCombosByViewState(states)

        withContext(Dispatchers.Main) { repos.player.insertNext(queueTrackCombos) }
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, states.size, states.size).umlautify()
        )
    }

    open fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks, context: Context) = TrackSelectionCallbacks(
        onAddToPlaylistClick = {
            launchOnIOThread { appCallbacks.onAddToPlaylistClick(Selection(tracks = listSelectedTracks())) }
        },
        onPlayClick = { playSelectedTracks() },
        onEnqueueClick = { enqueueSelectedTracks(context) },
        onUnselectAllClick = { unselectAllTracks() },
    )

    fun playPlaylist(playlistId: String, startTrackId: String? = null) = launchOnIOThread {
        val combos = getQueueTrackCombos(repos.playlist.listPlaylistTrackCombos(playlistId))
        val startIndex =
            startTrackId?.let { trackId -> combos.indexOfFirst { it.track.trackId == trackId }.takeIf { it > -1 } } ?: 0

        withContext(Dispatchers.Main) { repos.player.replaceAndPlay(combos, startIndex = startIndex) }
    }

    fun playTrack(state: Track.ViewState) = launchOnIOThread {
        getQueueTrackComboByViewState(state)?.also {
            withContext(Dispatchers.Main) { repos.player.insertNextAndPlay(it) }
        }
    }

    fun playTrackCombo(combo: AbstractTrackCombo) = launchOnIOThread {
        getQueueTrackCombo(combo)?.also {
            withContext(Dispatchers.Main) { repos.player.insertNextAndPlay(it) }
        }
    }

    fun playTrackCombos(combos: Collection<AbstractTrackCombo>, startIndex: Int = 0) = launchOnMainThread {
        repos.player.replaceAndPlay(withContext(Dispatchers.IO) { getQueueTrackCombos(combos) }, startIndex)
    }

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

    fun toggleTrackSelected(trackId: String) {
        repos.track.toggleTrackIdSelected(selectionKey, trackId)
    }

    fun unselectAllTracks() = repos.track.unselectAllTrackIds(selectionKey)

    fun unselectTracks(trackIds: Collection<String>) = repos.track.unselectTrackIds(selectionKey, trackIds)
}
