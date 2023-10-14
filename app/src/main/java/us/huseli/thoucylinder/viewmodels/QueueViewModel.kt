package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.TrackQueue
import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(private val repos: Repositories) : BaseViewModel(repos) {
    private val _selectedQueueTracks = MutableStateFlow<List<AbstractQueueTrack>>(emptyList())

    val selectedQueueTracks: StateFlow<List<AbstractQueueTrack>> = _selectedQueueTracks.asStateFlow()
    val queue: StateFlow<TrackQueue> = repos.player.queue
    val playerCurrentPojo: StateFlow<QueueTrackPojo?> = repos.player.currentPojo
    val playerPlaybackState: StateFlow<PlayerRepository.PlaybackState> = repos.player.playbackState
    val playerCurrentPositionMs: StateFlow<Long> = repos.player.currentPositionMs
    val playerCanGotoNext: StateFlow<Boolean> = repos.player.canGotoNext
    val playerCanGotoPrevious: StateFlow<Boolean> = repos.player.canGotoPrevious
    val playerCanPlay: StateFlow<Boolean> = repos.player.canPlay
    val playerIsRepeatEnabled = repos.player.isRepeatEnabled
    val playerIsShuffleEnabled = repos.player.isShuffleEnabled

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun removeFromQueue(queueTracks: List<AbstractQueueTrack>) = repos.player.removeFromQueue(queueTracks)

    fun seekTo(positionMs: Long) = repos.player.seekTo(positionMs)

    fun skipTo(index: Int) = repos.player.skipTo(index)

    fun skipToNext() = repos.player.skipToNext()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleSelected(queueTrack: AbstractQueueTrack) {
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
