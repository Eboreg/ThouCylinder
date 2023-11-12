package us.huseli.thoucylinder.repositories

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.abstr.toQueueTrackPojos
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.reindexed
import us.huseli.thoucylinder.dataclasses.pojos.toMediaItems
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
@MainThread
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueDao: QueueDao,
) : Player.Listener {
    enum class PlaybackState { STOPPED, PLAYING, PAUSED }
    enum class LastAction { PLAY, STOP, PAUSE }

    private val listeners = mutableListOf<PlayerRepositoryListener>()
    private val mutex = Mutex()
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionPollJob: Job? = null

    private val nextItemIndex: Int
        get() = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1

    private val _canGotoNext = MutableStateFlow(player.hasNextMediaItem())
    private val _canPlay = MutableStateFlow(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
    private val _currentPojo = MutableStateFlow<QueueTrackPojo?>(null)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _isRepeatEnabled = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    private val _queue = MutableStateFlow<List<QueueTrackPojo>>(emptyList())
    private var _lastAction = LastAction.STOP

    val canGotoNext = _canGotoNext.asStateFlow()
    val canPlay = _canPlay.asStateFlow()
    val currentPojo = _currentPojo.asStateFlow()
    val currentPositionMs = _currentPositionMs.asStateFlow()
    val isRepeatEnabled = _isRepeatEnabled.asStateFlow()
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()
    val playbackState = _playbackState.asStateFlow()
    val queue = _queue.asStateFlow()

    val lastAction: LastAction
        get() = _lastAction

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

    fun addListener(listener: PlayerRepositoryListener) = listeners.add(listener)

    fun insertNext(trackPojos: List<AbstractTrackPojo>) = insertTracksAt(trackPojos, nextItemIndex)

    fun insertNextAndPlay(pojo: AbstractTrackPojo) {
        val index = nextItemIndex
        val mediaItems = insertTracksAt(listOf(pojo), index)

        if (mediaItems.isNotEmpty()) {
            player.seekTo(index, 0L)
            play()
        }
    }

    fun moveNext(pojos: List<QueueTrackPojo>) {
        removeFromQueue(pojos)
        player.addMediaItems(nextItemIndex, pojos.map { it.toMediaItem() })
    }

    fun moveNextAndPlay(pojos: List<QueueTrackPojo>) {
        moveNext(pojos)
        player.seekTo(nextItemIndex, 0L)
        play()
    }

    fun onMoveTrackFinished(from: Int, to: Int) = player.moveMediaItem(from, to)

    fun play(index: Int? = null) {
        _lastAction = LastAction.PLAY
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        if (index != null) player.seekTo(index, 0L)
        player.play()
    }

    fun playOrPauseCurrent() {
        /** Plays/pauses the current item, if any. */
        if (player.isPlaying) {
            _lastAction = LastAction.PAUSE
            player.pause()
        } else play()
    }

    fun removeFromQueue(queueTracks: List<QueueTrackPojo>) {
        val ids = queueTracks.map { it.queueTrackId }
        val indices =
            _queue.value.mapIndexedNotNull { index, pojo -> if (ids.contains(pojo.queueTrackId)) index else null }

        indices.sortedDescending().forEach { index ->
            player.removeMediaItem(index)
        }
    }

    fun replaceAndPlay(trackPojos: List<AbstractTrackPojo>, startIndex: Int? = 0) {
        /** Clear queue, add tracks, play. */
        val pojos = trackPojos.toQueueTrackPojos()

        player.clearMediaItems()
        if (pojos.isNotEmpty()) {
            player.addMediaItems(pojos.map { it.toMediaItem() })
            player.seekTo(max(startIndex ?: 0, 0), 0L)
            play()
        }
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun skipTo(index: Int) {
        if (player.mediaItemCount > index) {
            player.seekTo(index, 0L)
            play()
        }
    }

    fun skipToNext() {
        if (player.hasNextMediaItem()) {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.seekToNextMediaItem()
        }
    }

    fun skipToStartOrPrevious() {
        if (player.currentPosition > 5000 || !player.hasPreviousMediaItem()) player.seekTo(0L)
        else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.seekToPreviousMediaItem()
        }
    }

    fun toggleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun updateTrack(pojo: QueueTrackPojo) {
        player.removeMediaItem(pojo.position)
        player.addMediaItem(pojo.position, pojo.toMediaItem())
        if (pojo.position <= player.currentMediaItemIndex && _playbackState.value != PlaybackState.PLAYING)
            player.seekTo(player.currentMediaItemIndex - 1, 0L)
    }

    /** PRIVATE METHODS ******************************************************/

    private fun findQueueItemByMediaItem(mediaItem: MediaItem?): QueueTrackPojo? =
        mediaItem?.mediaId?.let { itemId -> _queue.value.find { it.queueTrackId.toString() == itemId } }

    private fun insertTracksAt(trackPojos: List<AbstractTrackPojo>, index: Int): List<MediaItem> {
        val mediaItems = trackPojos.toQueueTrackPojos(startIndex = index).toMediaItems()

        if (mediaItems.isNotEmpty()) player.addMediaItems(index, mediaItems)
        return mediaItems
    }

    private fun saveQueueIndex(value: Int) {
        preferences.edit().putInt(PREF_QUEUE_INDEX, value).apply()
    }

    /** OVERRIDDEN METHODS ***************************************************/

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        _canGotoNext.value = availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        _canPlay.value = availableCommands.contains(Player.COMMAND_PLAY_PAUSE)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) Log.i(javaClass.simpleName, "Playing: ${player.currentMediaItem?.localConfiguration?.uri}")
        _playbackState.value =
            if (isPlaying) PlaybackState.PLAYING
            else if (player.playbackState == Player.STATE_READY && !player.playWhenReady) PlaybackState.PAUSED
            else PlaybackState.STOPPED
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Called when player's currently playing MediaItem changes.
        scope.launch {
            mutex.withLock {
                saveQueueIndex(player.currentMediaItemIndex)
                val pojo = findQueueItemByMediaItem(mediaItem)
                if (pojo != _currentPojo.value) {
                    Log.i("PlayerRepository", "current track URI: ${pojo?.uri}")
                    _currentPojo.value = pojo
                    _currentPositionMs.value = player.currentPosition
                }
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
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) scope.launch {
            mutex.withLock {
                val queue = mutableListOf<QueueTrackPojo>()

                if (timeline.windowCount > 0) {
                    val window = Timeline.Window()
                    for (idx in 0 until timeline.windowCount) {
                        timeline.getWindow(idx, window)
                        window.mediaItem.localConfiguration?.tag?.also { tag ->
                            if (tag is QueueTrackPojo) queue.add(tag)
                        }
                    }
                }

                val queueReindexed = queue.reindexed()
                Log.i("PlayerRepository", "onTimelineChanged: queueReindexed=$queueReindexed")

                _queue.value = queueReindexed
                queueDao.setQueue(queueReindexed)
                saveQueueIndex(player.currentMediaItemIndex)
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        listeners.forEach { it.onPlayerError(error, _currentPojo.value, _lastAction) }
    }
}
