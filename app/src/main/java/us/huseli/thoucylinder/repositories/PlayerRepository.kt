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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.Constants.PREF_CURRENT_TRACK_POSITION
import us.huseli.thoucylinder.Constants.PREF_QUEUE_INDEX
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.database.QueueDao
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.pojos.containsWithPosition
import us.huseli.thoucylinder.dataclasses.pojos.reindexed
import us.huseli.thoucylinder.dataclasses.pojos.toMediaItems
import us.huseli.thoucylinder.filterValuesNotNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

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
    private var positionUpdateJob: Job? = null
    private var positionSaveJob: Job? = null
    private var currentTrackPlayTimeJob: Job? = null

    private val _canGotoNext = MutableStateFlow(player.hasNextMediaItem())
    private val _canPlay = MutableStateFlow(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
    private val _currentPojo = MutableStateFlow<QueueTrackPojo?>(null)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _isRepeatEnabled = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    private val _queue = MutableStateFlow<List<QueueTrackPojo>>(emptyList())
    private var _lastAction = LastAction.STOP
    private val _isCurrentTrackHalfPlayedReported = MutableStateFlow(false)
    private val _currentTrackPlayTime = MutableStateFlow(0) // seconds
    private val _currentTrackPlayStartTimestamp = MutableStateFlow<Long?>(null)

    val canGotoNext: StateFlow<Boolean> = _canGotoNext.asStateFlow()
    val canPlay: StateFlow<Boolean> = _canPlay.asStateFlow()
    val currentPojo: StateFlow<QueueTrackPojo?> = _currentPojo.asStateFlow()
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    val queue: StateFlow<List<QueueTrackPojo>> = _queue.asStateFlow()

    val nextItemIndex: Int
        get() = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1

    init {
        player.addListener(this)
        player.prepare()

        scope.launch {
            /**
             * Only use queue data from the database in order to bootstrap the queue on app start. Thereafter, data
             * flows unidirectionally _from_ ExoPlayer _to_ the database, basically making Exoplayer instance the
             * single source of truth for queue contents and playback status etc, with some stateflows acting as cache.
             */
            mutex.withLock {
                val pojos = queueDao.getQueue()
                val currentIndex =
                    preferences.getInt(PREF_QUEUE_INDEX, 0).takeIf { it < pojos.size && it > -1 } ?: 0
                val currentTrackPosition = preferences.getLong(PREF_CURRENT_TRACK_POSITION, 0)

                _queue.value = pojos

                if (pojos.isNotEmpty()) {
                    player.addMediaItems(pojos.map { it.toMediaItem() })
                    player.seekTo(currentIndex, currentTrackPosition)
                }
            }
        }

        scope.launch {
            /**
             * However, continuously monitor _track_ data from the database, in case URIs change because a track has
             * been downloaded or locally deleted.
             */
            queueDao.flowTracksInQueue().distinctUntilChanged().collect { tracks ->
                mutex.withLock {
                    val uriMap =
                        _queue.value.associateWith { pojo -> tracks.find { it.trackId == pojo.track.trackId }?.playUri }

                    // Handle updated URIs:
                    uriMap.filterValuesNotNull().forEach { (pojo, uri) ->
                        if (uri != pojo.uri) updateTrack(pojo.copy(uri = uri))
                    }
                }
            }
        }

        scope.launch {
            _playbackState.collect { state ->
                if (state == PlaybackState.PLAYING) {
                    positionUpdateJob = launch {
                        while (true) {
                            _currentPositionMs.value = player.currentPosition
                            delay(500)
                        }
                    }
                    positionSaveJob = launch {
                        while (true) {
                            saveCurrentPosition()
                            delay(5000)
                        }
                    }
                    currentTrackPlayTimeJob = launch {
                        while (true) {
                            if (!_isCurrentTrackHalfPlayedReported.value) {
                                val pojo = _currentPojo.value
                                val duration = pojo?.track?.duration

                                if (pojo != null && duration != null) {
                                    // Scrobble when the track has been played for at least half its duration, or for 4
                                    // minutes (whichever occurs earlier).
                                    // https://www.last.fm/api/scrobbling#when-is-a-scrobble-a-scrobble
                                    val threshold = min(duration.inWholeSeconds / 2, 240L)

                                    if (_currentTrackPlayTime.value >= threshold) {
                                        _currentTrackPlayStartTimestamp.value?.also { startTimestamp ->
                                            listeners.forEach { it.onHalfTrackPlayed(pojo, startTimestamp) }
                                        }
                                        _isCurrentTrackHalfPlayedReported.value = true
                                    }
                                }
                            }
                            delay(5000)
                            _currentTrackPlayTime.value += 5
                        }
                    }
                    if (_currentTrackPlayStartTimestamp.value == null) {
                        _currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                    }
                } else {
                    positionUpdateJob?.cancel()
                    positionUpdateJob = null
                    positionSaveJob?.cancel()
                    positionSaveJob = null
                    currentTrackPlayTimeJob?.cancel()
                    currentTrackPlayTimeJob = null
                }

                listeners.forEach { it.onPlaybackChange(_currentPojo.value, state) }
            }
        }

        scope.launch {
            _currentPojo.collect { pojo ->
                _currentTrackPlayTime.value = 0
                _isCurrentTrackHalfPlayedReported.value = false
                listeners.forEach { it.onPlaybackChange(pojo, _playbackState.value) }
            }
        }
    }

    fun addListener(listener: PlayerRepositoryListener) = listeners.add(listener)

    fun insertNext(pojos: List<QueueTrackPojo>) {
        if (pojos.isNotEmpty()) player.addMediaItems(nextItemIndex, pojos.toMediaItems())
    }

    fun insertNextAndPlay(pojo: QueueTrackPojo) {
        player.addMediaItem(nextItemIndex, pojo.toMediaItem())
        player.seekTo(nextItemIndex, 0L)
        play()
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

    fun removeFromQueue(pojos: List<QueueTrackPojo>) = scope.launch {
        mutex.withLock {
            val ids = pojos.map { it.queueTrackId }
            val indices =
                _queue.value.mapIndexedNotNull { index, pojo -> if (ids.contains(pojo.queueTrackId)) index else null }

            indices.sortedDescending().forEach { index ->
                player.removeMediaItem(index)
            }
        }
    }

    fun replaceAndPlay(pojos: List<QueueTrackPojo>, startIndex: Int? = 0) {
        /** Clear queue, add tracks, play. */
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

    private fun saveCurrentPosition() =
        preferences.edit().putLong(PREF_CURRENT_TRACK_POSITION, player.currentPosition).apply()

    private fun saveQueueIndex() =
        preferences.edit().putInt(PREF_QUEUE_INDEX, player.currentMediaItemIndex).apply()

    /** OVERRIDDEN METHODS ***************************************************/

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        _canGotoNext.value = availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        _canPlay.value = availableCommands.contains(Player.COMMAND_PLAY_PAUSE)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) Log.i(javaClass.simpleName, "Playing: ${player.currentMediaItem?.localConfiguration?.uri}")
        player.currentMediaItem?.requestMetadata?.mediaUri
        _playbackState.value =
            if (isPlaying) {
                if (_currentTrackPlayStartTimestamp.value == null)
                    _currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                PlaybackState.PLAYING
            }
            else if (player.playbackState == Player.STATE_READY && !player.playWhenReady) PlaybackState.PAUSED
            else PlaybackState.STOPPED
        saveCurrentPosition()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Called when player's currently playing MediaItem changes.
        scope.launch {
            mutex.withLock {
                saveCurrentPosition()
                saveQueueIndex()
                val pojo = findQueueItemByMediaItem(mediaItem)
                if (pojo != _currentPojo.value) {
                    Log.i("PlayerRepository", "current track URI: ${pojo?.uri}")
                    _currentPojo.value = pojo
                    _currentPositionMs.value = player.currentPosition
                    if (_playbackState.value == PlaybackState.PLAYING)
                        _currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                    else _currentTrackPlayStartTimestamp.value = null
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
                val newAndChanged = queueReindexed.filter { !_queue.value.containsWithPosition(it) }
                val removed = _queue.value.filter { !queueReindexed.contains(it) }
                Log.i("PlayerRepository", "onTimelineChanged: queueReindexed=$queueReindexed")

                if (newAndChanged.isNotEmpty())
                    queueDao.upsertQueueTracks(*newAndChanged.map { it.queueTrack }.toTypedArray())
                if (removed.isNotEmpty())
                    queueDao.deleteQueueTracks(*removed.map { it.queueTrack }.toTypedArray())
                _queue.value = queueReindexed
                saveCurrentPosition()
                saveQueueIndex()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        listeners.forEach { it.onPlayerError(error, _currentPojo.value, _lastAction) }
    }
}
