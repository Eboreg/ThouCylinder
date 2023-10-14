package us.huseli.thoucylinder.repositories

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.Constants.PREF_QUEUE_INDEX
import us.huseli.thoucylinder.database.QueueDao
import us.huseli.thoucylinder.dataclasses.TrackQueue
import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.toQueueTrackPojos
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueDao: QueueDao,
) : Player.Listener {
    enum class PlaybackState { STOPPED, PLAYING, PAUSED }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionPollJob: Job? = null
    private val mutex = Mutex()
    private val _queue = MutableStateFlow(TrackQueue())

    private val _currentPojo = MutableStateFlow<QueueTrackPojo?>(null)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    private val _canGotoNext = MutableStateFlow(player.hasNextMediaItem())
    private val _canGotoPrevious = MutableStateFlow(player.hasPreviousMediaItem())
    private val _canPlay = MutableStateFlow(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
    private val _isRepeatEnabled = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)

    val currentPositionMs = _currentPositionMs.asStateFlow()
    val playbackState = _playbackState.asStateFlow()
    val currentPojo = _currentPojo.asStateFlow()
    val canGotoNext = _canGotoNext.asStateFlow()
    val canGotoPrevious = _canGotoPrevious.asStateFlow()
    val canPlay = _canPlay.asStateFlow()
    val queue = _queue.asStateFlow()
    val isRepeatEnabled = _isRepeatEnabled.asStateFlow()
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    init {
        player.addListener(this)
        player.prepare()

        scope.launch {
            /**
             * Only fetch the queue from DB once at startup (but save it back on updates). Thereafter, the Exoplayer
             * instance is basically the single source of truth for queue contents and playback status etc, with some
             * stateflows acting as cache.
             */
            mutex.withLock {
                val pojos = queueDao.getQueue()
                val currentIndex =
                    preferences.getInt(PREF_QUEUE_INDEX, 0).takeIf { it < pojos.size && it > -1 } ?: 0

                if (pojos.isNotEmpty()) {
                    player.addMediaItems(pojos.map { it.toMediaItem() })
                    player.seekTo(currentIndex, 0L)
                }
            }
        }

        scope.launch {
            _playbackState.collect { state ->
                if (state == PlaybackState.PLAYING) {
                    positionPollJob = launch {
                        while (true) {
                            _currentPositionMs.value = player.currentPosition
                            delay(500)
                        }
                    }
                } else {
                    positionPollJob?.cancel()
                    positionPollJob = null
                }
            }
        }
    }

    fun insertNext(track: Track) = insertNext(listOf(track))

    fun insertNext(tracks: List<Track>) = scope.launch {
        val startIdx = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        val mediaItems =
            tracks.mapIndexedNotNull { idx, track -> track.toQueueTrackPojo(idx + startIdx)?.toMediaItem() }

        if (mediaItems.isNotEmpty()) player.addMediaItems(startIdx, mediaItems)
    }

    /** Insert track in queue right after at current position and play it. */
    fun insertAndPlay(track: Track) = scope.launch {
        val index = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
        val pojo = track.toQueueTrackPojo(index)

        if (pojo != null) {
            player.addMediaItem(index, pojo.toMediaItem())
            player.seekTo(index, 0L)
            player.play()
        }
    }

    /** Plays/pauses the current item, if any. */
    fun playOrPauseCurrent() = scope.launch {
        if (player.isPlaying) player.pause()
        else player.play()
    }

    fun removeFromQueue(queueTracks: List<AbstractQueueTrack>) {
        val indices = _queue.value.getIndices(queueTracks)

        indices.sortedDescending().forEach { index ->
            player.removeMediaItem(index)
        }
    }

    /** Clear queue, add tracks, play. */
    fun replaceAndPlay(tracks: List<Track>, startIndex: Int? = null) = scope.launch {
        val pojos = tracks.toQueueTrackPojos()

        player.clearMediaItems()
        if (pojos.isNotEmpty()) {
            player.addMediaItems(pojos.map { it.toMediaItem() })
            player.seekTo(max(startIndex ?: 0, 0), 0L)
            player.play()
        }
    }

    fun seekTo(positionMs: Long) = scope.launch {
        player.seekTo(positionMs)
    }

    fun skipTo(index: Int) = scope.launch {
        if (player.mediaItemCount > index) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun skipToNext() = scope.launch {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun skipToStartOrPrevious() = scope.launch {
        if (player.currentPosition > 5000 || !player.hasPreviousMediaItem()) player.seekTo(0L)
        else player.seekToPreviousMediaItem()
    }

    fun toggleRepeat() = scope.launch {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() = scope.launch {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    /** PRIVATE METHODS ******************************************************/
    private suspend fun findQueueItemByMediaItem(mediaItem: MediaItem?): QueueTrackPojo? = mutex.withLock {
        mediaItem?.mediaId?.let { itemId -> _queue.value.find { it.queueTrackId.toString() == itemId } }
    }

    private fun saveQueueIndex(value: Int) {
        preferences.edit().putInt(PREF_QUEUE_INDEX, value).apply()
    }

    /** OVERRIDDEN METHODS ***************************************************/
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        _canGotoNext.value = availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        _canGotoPrevious.value = availableCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        _canPlay.value = availableCommands.contains(Player.COMMAND_PLAY_PAUSE)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _playbackState.value =
            if (isPlaying) PlaybackState.PLAYING
            else if (player.playbackState == Player.STATE_READY && !player.playWhenReady) PlaybackState.PAUSED
            else PlaybackState.STOPPED
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Called when player's currently playing MediaItem changes.
        saveQueueIndex(player.currentMediaItemIndex)
        scope.launch {
            val pojo = findQueueItemByMediaItem(mediaItem)
            if (pojo != _currentPojo.value) {
                _currentPojo.value = pojo
                _currentPositionMs.value = player.currentPosition
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        _isRepeatEnabled.value = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> false
            else -> true
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        _isShuffleEnabled.value = shuffleModeEnabled
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            val queueTracks = mutableListOf<QueueTrackPojo>()

            if (timeline.windowCount > 0) {
                val window = Timeline.Window()
                for (idx in 0 until timeline.windowCount) {
                    timeline.getWindow(idx, window)
                    window.mediaItem.localConfiguration?.tag?.also { tag ->
                        if (tag is QueueTrackPojo) queueTracks.add(tag)
                    }
                }
            }

            val queue = TrackQueue(queueTracks)
            _queue.value = queue
            scope.launch {
                queueDao.setQueue(queue.items)
            }
        }
    }
}
