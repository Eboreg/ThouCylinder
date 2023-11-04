package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class QueueViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _selectedQueueTracks = MutableStateFlow<List<QueueTrackPojo>>(emptyList())
    private val _queue = MutableStateFlow<List<QueueTrackPojo>>(emptyList())

    val playerCurrentPojo: StateFlow<QueueTrackPojo?> = repos.player.currentPojo
    val playerPlaybackState: StateFlow<PlayerRepository.PlaybackState> = repos.player.playbackState
    val playerCurrentPositionMs: StateFlow<Long> = repos.player.currentPositionMs
    val playerCanGotoNext: StateFlow<Boolean> = repos.player.canGotoNext
    val playerCanPlay: StateFlow<Boolean> = repos.player.canPlay
    val playerIsRepeatEnabled = repos.player.isRepeatEnabled
    val playerIsShuffleEnabled = repos.player.isShuffleEnabled
    val queue = _queue.asStateFlow()
    val selectedQueueTracks: StateFlow<List<QueueTrackPojo>> = _selectedQueueTracks.asStateFlow()

    init {
        viewModelScope.launch {
            repos.player.queue.collect { queue -> _queue.value = queue }
        }
    }

    fun onMoveTrack(from: Int, to: Int) {
        /**
         * Only does visual move while reordering, does not store anything. Call onMoveTrackFinished() when reorder
         * operation is finished.
         */
        _queue.value = _queue.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = repos.player.onMoveTrackFinished(from, to)

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun playQueueTracks(pojos: List<QueueTrackPojo>) = repos.player.moveNextAndPlay(pojos)

    fun playQueueTracksNext(pojos: List<QueueTrackPojo>, context: Context) {
        repos.player.moveNext(pojos)
        SnackbarEngine.addInfo(
            context.resources.getQuantityString(R.plurals.x_tracks_enqueued_next, pojos.size, pojos.size)
        )
    }

    fun removeFromQueue(queueTracks: List<QueueTrackPojo>) = repos.player.removeFromQueue(queueTracks)

    fun seekTo(positionMs: Long) = repos.player.seekTo(positionMs)

    fun selectQueueTracksFromLastSelected(to: QueueTrackPojo) = viewModelScope.launch {
        val lastSelected = _selectedQueueTracks.value.lastOrNull()

        if (lastSelected != null) {
            val thisIdx = queue.value.indexOf(to)
            val lastSelectedIdx = queue.value.indexOf(lastSelected)

            _selectedQueueTracks.value +=
                queue.value.subList(min(thisIdx, lastSelectedIdx), max(thisIdx, lastSelectedIdx) + 1)
        } else {
            _selectedQueueTracks.value += to
        }
    }

    fun skipTo(index: Int) = repos.player.skipTo(index)

    fun skipToNext() = repos.player.skipToNext()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleSelected(queueTrack: QueueTrackPojo) {
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
