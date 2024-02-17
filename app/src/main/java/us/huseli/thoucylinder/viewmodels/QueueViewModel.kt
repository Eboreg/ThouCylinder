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
import us.huseli.thoucylinder.umlautify
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.time.DurationUnit

@HiltViewModel
class QueueViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
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
    val selectedQueueTracks: Flow<List<QueueTrackCombo>> = combine(_queue, _selectedQueueTracks) { queue, selected ->
        selected.filter { queue.contains(it) }
    }
    val trackDownloadTasks = repos.download.tasks

    init {
        viewModelScope.launch {
            repos.player.queue.collect { queue -> _queue.value = queue }
        }
    }

    fun enqueueQueueTracks(combos: List<QueueTrackCombo>, context: Context) {
        repos.player.moveNext(combos)
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, combos.size, combos.size).umlautify()
        )
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

    fun playQueueTracks(combos: List<QueueTrackCombo>) = repos.player.moveNextAndPlay(combos)

    fun removeFromQueue(combo: QueueTrackCombo) = removeFromQueue(listOf(combo))

    fun removeFromQueue(combos: List<QueueTrackCombo>) {
        repos.player.removeFromQueue(combos)
        _selectedQueueTracks.value -= combos
    }

    fun seekToProgress(progress: Float) = repos.player.seekToProgress(progress)

    fun selectQueueTracksFromLastSelected(to: QueueTrackCombo) {
        val lastSelectedIdx = queue.value.indexOf(_selectedQueueTracks.value.lastOrNull())
        val thisIdx = queue.value.indexOf(to)

        if (lastSelectedIdx > -1 && thisIdx > -1) {
            _selectedQueueTracks.value +=
                queue.value.subList(min(thisIdx, lastSelectedIdx), max(thisIdx, lastSelectedIdx) + 1)
        } else {
            _selectedQueueTracks.value += to
        }
    }

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

    fun unselectAllQueueTracks() {
        _selectedQueueTracks.value = emptyList()
    }
}
