package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnMainThread
import us.huseli.thoucylinder.umlautify
import java.util.UUID
import javax.inject.Inject
import kotlin.time.DurationUnit

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractTrackListViewModel("QueueViewModel", repos) {
    private val _selectedQueueTracks = MutableStateFlow<List<QueueTrackCombo>>(emptyList())
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())

    val canGotoNext = repos.player.canGotoNext
    val canPlay = repos.player.canPlay
    val currentCombo = repos.player.currentCombo.filterNotNull().distinctUntilChanged()
    val currentPositionSeconds = repos.player.currentPositionMs.map { it / 1000 }.distinctUntilChanged()
    val currentProgress: Flow<Float> = combine(repos.player.currentPositionMs, currentCombo) { position, combo ->
        val endPosition = combo.track.metadata?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }
        endPosition?.let { position / it.toFloat() } ?: 0f
    }.distinctUntilChanged()
    val isLoading = repos.player.isLoading
    val isPlaying = repos.player.isPlaying
    val isRepeatEnabled = repos.player.isRepeatEnabled
    val isShuffleEnabled = repos.player.isShuffleEnabled
    val queue = _queue.asStateFlow()
    override val trackDownloadTasks = repos.download.tasks

    init {
        viewModelScope.launch {
            repos.player.queue.collect { queue -> _queue.value = queue }
        }
    }

    fun getNextCombo(): QueueTrackCombo? = repos.player.getNextTrack()

    fun getPreviousCombo(): QueueTrackCombo? = repos.player.getPreviousTrack()

    fun onMoveTrack(from: Int, to: Int) {
        /**
         * Only does visual move while reordering, does not store anything. Call onMoveTrackFinished() when reorder
         * operation is finished.
         */
        _queue.value = _queue.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = repos.player.onMoveTrackFinished(from, to)

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun removeFromQueue(queueTrackId: UUID) {
        repos.player.removeFromQueue(listOf(queueTrackId))
        unselectTracks(listOf(queueTrackId))
    }

    fun removeSelectedTracksFromQueue() {
        repos.player.removeFromQueue(selectedTrackIds.value)
        unselectAllTracks()
    }

    fun seekToProgress(progress: Float) = repos.player.seekToProgress(progress)

    fun skipTo(index: Int) = repos.player.skipTo(index)

    fun skipToNext() = repos.player.skipToNext()

    fun skipToPrevious() = repos.player.skipToPrevious()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleSelected(queueTrack: QueueTrackCombo) {
        if (_selectedQueueTracks.value.contains(queueTrack))
            _selectedQueueTracks.value -= queueTrack
        else
            _selectedQueueTracks.value += queueTrack
    }

    fun toggleShuffle() = repos.player.toggleShuffle()

    override suspend fun listSelectedTrackCombos(): List<QueueTrackCombo> =
        repos.player.listQueueTrackCombosById(selectedTrackIds.value)

    override suspend fun listSelectedTracks(): List<Track> = listSelectedTrackCombos().map { it.track }

    override fun enqueueSelectedTracks(context: Context) {
        launchOnMainThread {
            repos.player.moveNext(selectedTrackIds.value)
            SnackbarEngine.addInfo(
                context.resources.getQuantityString(
                    R.plurals.x_tracks_enqueued_next,
                    selectedTrackIds.value.size,
                    selectedTrackIds.value.size,
                ).umlautify()
            )
        }
    }

    override fun playSelectedTracks() {
        launchOnMainThread { repos.player.moveNextAndPlay(selectedTrackIds.value) }
    }
}
