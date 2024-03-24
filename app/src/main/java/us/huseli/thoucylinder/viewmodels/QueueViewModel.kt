package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.views.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.RadioPojo
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.launchOnMainThread
import us.huseli.thoucylinder.umlautify
import java.util.UUID
import javax.inject.Inject
import kotlin.time.DurationUnit

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repos: Repositories,
) : AbstractTrackListViewModel("QueueViewModel", repos) {
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())

    val canGotoNext: StateFlow<Boolean> = repos.player.canGotoNext
    val canPlay: Flow<Boolean> = repos.player.canPlay
    val currentCombo: Flow<QueueTrackCombo> = repos.player.currentCombo.filterNotNull().distinctUntilChanged()
    val currentPositionSeconds: Flow<Long> = repos.player.currentPositionMs.map { it / 1000 }.distinctUntilChanged()
    val currentProgress: Flow<Float> = combine(repos.player.currentPositionMs, currentCombo) { position, combo ->
        val endPosition = combo.track.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }
        endPosition?.let { position / it.toFloat() } ?: 0f
    }.distinctUntilChanged()
    val isLoading: StateFlow<Boolean> = repos.player.isLoading
    val isPlaying: Flow<Boolean> = repos.player.isPlaying
    val isRepeatEnabled: StateFlow<Boolean> = repos.player.isRepeatEnabled
    val isShuffleEnabled: StateFlow<Boolean> = repos.player.isShuffleEnabled
    val queue: StateFlow<List<QueueTrackCombo>> = _queue.asStateFlow()
    val radioPojo: Flow<RadioPojo?> = repos.player.radioPojo

    override val trackDownloadTasks: Flow<List<TrackDownloadTask>> = repos.download.tasks

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

    fun toggleShuffle() = repos.player.toggleShuffle()

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

    override fun getTrackSelectionCallbacks(appCallbacks: AppCallbacks, context: Context): TrackSelectionCallbacks {
        /** It makes little sense to define onPlayClick and onEnqueueClick here. */
        return TrackSelectionCallbacks(
            onAddToPlaylistClick = {
                launchOnIOThread { appCallbacks.onAddToPlaylistClick(Selection(tracks = listSelectedTracks())) }
            },
            onUnselectAllClick = { unselectAllTracks() },
            onSelectAllClick = { repos.track.selectTrackIds("QueueViewModel", _queue.value.map { it.queueTrackId }) },
        )
    }

    override suspend fun listSelectedTrackCombos(): List<QueueTrackCombo> =
        repos.player.listQueueTrackCombosById(selectedTrackIds.value)

    override suspend fun listSelectedTracks(): List<Track> = listSelectedTrackCombos().map { it.track }

    override fun playSelectedTracks() {
        launchOnMainThread { repos.player.moveNextAndPlay(selectedTrackIds.value) }
    }
}
