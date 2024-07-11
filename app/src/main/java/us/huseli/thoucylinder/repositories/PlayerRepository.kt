package us.huseli.thoucylinder.repositories

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.glance.appwidget.updateAll
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.retaintheme.extensions.splitIntervals
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.Constants.PREF_CURRENT_TRACK_POSITION
import us.huseli.thoucylinder.Constants.PREF_QUEUE_INDEX
import us.huseli.thoucylinder.PlaybackService
import us.huseli.thoucylinder.database.QueueDao
import us.huseli.thoucylinder.dataclasses.track.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.track.containsWithPosition
import us.huseli.thoucylinder.dataclasses.track.reindexed
import us.huseli.thoucylinder.enums.PlaybackState
import us.huseli.thoucylinder.interfaces.ILogger
import us.huseli.thoucylinder.interfaces.PlayerRepositoryListener
import us.huseli.thoucylinder.widget.AppWidget
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.time.DurationUnit

@Singleton
@MainThread
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueDao: QueueDao,
) : Player.Listener, ILogger, AbstractScopeHolder() {
    enum class LastAction { PLAY, STOP, PAUSE }

    private val listeners = mutableListOf<PlayerRepositoryListener>()
    private val queueMutex = Mutex()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var lastAction = LastAction.STOP
    private var onPlayerReady: (MediaController) -> Unit = {}
    private var player: MediaController? = null

    private val currentTrackPlayStartTimestamp = MutableStateFlow<Long?>(null)
    private val currentTrackPlayTime = MutableStateFlow(0) // seconds
    private val isCurrentTrackHalfPlayedReported = MutableStateFlow(false)
    private val playbackState = MutableStateFlow(PlaybackState.STOPPED)
    private val playedQueueTrackIds = MutableStateFlow<Set<String>>(emptySet())

    private val _canGotoNext = MutableStateFlow(player?.hasNextMediaItem() ?: false)
    private val _canGotoPrevious = MutableStateFlow(player?.hasPreviousMediaItem() ?: false)
    private val _canPlay = MutableStateFlow(player?.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) ?: false)
    private val _currentCombo = MutableStateFlow<QueueTrackCombo?>(null)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _isLoading = MutableStateFlow(false)
    private val _isRepeatEnabled = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _nextCombo = MutableStateFlow<QueueTrackCombo?>(null)
    private val _previousCombo = MutableStateFlow<QueueTrackCombo?>(null)
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())

    val canGotoNext: StateFlow<Boolean> = _canGotoNext.asStateFlow()
    val canGotoPrevious: StateFlow<Boolean> = _canGotoPrevious.asStateFlow()
    val canPlay = combine(_canPlay, _isLoading) { canPlay, isLoading -> canPlay && !isLoading }.distinctUntilChanged()
    val currentCombo: StateFlow<QueueTrackCombo?> = _currentCombo.asStateFlow()
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isPlaying: Flow<Boolean> = playbackState.map { it == PlaybackState.PLAYING }.distinctUntilChanged()
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()
    val nextCombo = _nextCombo.asStateFlow()
    val previousCombo = _previousCombo.asStateFlow()
    val queue: StateFlow<List<QueueTrackCombo>> = _queue.asStateFlow()
    val replaceSignal = Channel<Boolean>()
    val trackCount = _queue.map { it.size }

    val tracksLeft = combine(playedQueueTrackIds, _queue) { playedIds, queue ->
        playedIds.filter { id -> queue.map { it.queueTrackId }.contains(id) }.toSet().sorted()
    }.map { playedIds ->
        if (_isShuffleEnabled.value) _queue.value.size - playedIds.size
        else _queue.value.size - (player?.currentMediaItemIndex?.plus(1) ?: 0)
    }.distinctUntilChanged()

    val nextItemIndex: Int
        get() = player?.let { if (it.mediaItemCount == 0) 0 else it.currentMediaItemIndex + 1 } ?: 0

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                player = controllerFuture.get().also {
                    it.addListener(this)
                    it.prepare()
                    onPlayerReady(it)
                }
            },
            MoreExecutors.directExecutor(),
        )

        launchOnMainThread {
            /**
             * Only use queue data from the database in order to bootstrap the queue on app start. Thereafter, data
             * flows unidirectionally _from_ ExoPlayer _to_ the database, basically making Exoplayer instance the
             * single source of truth for queue contents and playback status etc, with some stateflows acting as cache.
             */
            queueMutex.withLock {
                val combos = onIOThread { queueDao.getQueue() }
                val currentIndex =
                    preferences.getInt(PREF_QUEUE_INDEX, 0).takeIf { it < combos.size && it > -1 } ?: 0
                val currentTrackPosition = preferences.getLong(PREF_CURRENT_TRACK_POSITION, 0)

                _currentPositionMs.value = currentTrackPosition
                _queue.value = combos

                if (combos.isNotEmpty()) {
                    val func = { player: MediaController ->
                        player.setMediaItems(
                            combos.map { it.toMediaItem() },
                            currentIndex,
                            currentTrackPosition,
                        )
                    }

                    player?.also(func) ?: run { onPlayerReady = func }
                }
            }
        }

        launchOnMainThread {
            /**
             * However, continuously monitor _track_ data from the database, in case URIs change because a track has
             * been downloaded, locally deleted, got their Youtube URLs updated, etc.
             */
            queueDao.flowTracksInQueue().distinctUntilChanged().collect { tracks ->
                queueMutex.withLock {
                    val uriMap = _queue.value.associateWith { combo ->
                        tracks.find { it.trackId == combo.track.trackId }?.playUri
                    }

                    // Handle updated URIs:
                    uriMap.filterValuesNotNull().forEach { (combo, uri) ->
                        if (uri != combo.uri) updateTrack(combo.copy(uri = uri))
                    }
                }
            }
        }

        launchOnMainThread {
            playbackState.collectLatest { state ->
                if (state == PlaybackState.PLAYING) while (true) {
                    player?.currentPosition?.also { _currentPositionMs.value = it }
                    delay(500)
                }
            }
        }

        launchOnMainThread {
            playbackState.collectLatest {
                if (it == PlaybackState.PLAYING) while (true) {
                    saveCurrentPosition()
                    delay(5000)
                }
            }
        }

        launchOnMainThread {
            playbackState.collectLatest { state ->
                if (state == PlaybackState.PLAYING) while (true) {
                    if (!isCurrentTrackHalfPlayedReported.value) {
                        val combo = _currentCombo.value
                        val duration = combo?.track?.duration

                        if (combo != null && duration != null) {
                            // Scrobble when the track has been played for at least half its duration, or for 4
                            // minutes (whichever occurs earlier).
                            // https://www.last.fm/api/scrobbling#when-is-a-scrobble-a-scrobble
                            val threshold = min(duration.inWholeSeconds / 2, 240L)

                            if (currentTrackPlayTime.value >= threshold) {
                                currentTrackPlayStartTimestamp.value?.also { startTimestamp ->
                                    listeners.forEach { it.onHalfTrackPlayed(combo, startTimestamp) }
                                }
                                isCurrentTrackHalfPlayedReported.value = true
                            }
                        }
                    }
                    delay(5000)
                    currentTrackPlayTime.value += 5
                }
            }
        }

        launchOnMainThread {
            playbackState.collect { state ->
                if (state == PlaybackState.PLAYING && currentTrackPlayStartTimestamp.value == null) {
                    currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                }

                listeners.forEach { it.onPlaybackChange(_currentCombo.value, state) }
                updateWidget()
            }
        }

        launchOnMainThread {
            _currentCombo.collect { combo ->
                currentTrackPlayTime.value = 0
                isCurrentTrackHalfPlayedReported.value = false
                listeners.forEach { it.onPlaybackChange(combo, playbackState.value) }
                updateWidget()
            }
        }
    }

    fun addListener(listener: PlayerRepositoryListener) = listeners.add(listener)

    fun clearQueue() {
        player?.clearMediaItems()
    }

    fun insert(combo: QueueTrackCombo, position: Int) {
        player?.addMediaItem(position, combo.toMediaItem())
    }

    fun insertLast(combo: QueueTrackCombo) {
        player?.run { addMediaItem(mediaItemCount, combo.copy(position = mediaItemCount).toMediaItem()) }
    }

    fun insertLastAndPlay(combo: QueueTrackCombo) {
        player?.also {
            val index = it.mediaItemCount

            it.addMediaItem(index, combo.copy(position = index).toMediaItem())
            play(index)
        }
    }

    fun insertNextAndPlay(combo: QueueTrackCombo) {
        val index = nextItemIndex

        player?.addMediaItem(index, combo.copy(position = index).toMediaItem())
        play(index)
    }

    fun moveNext(queueTrackIds: Collection<String>) {
        val queueTrackCombos = _queue.value.filter { queueTrackIds.contains(it.queueTrackId) }

        removeFromQueue(queueTrackIds)
        player?.addMediaItems(nextItemIndex, queueTrackCombos.map { it.toMediaItem() })
    }

    fun moveNextAndPlay(queueTrackIds: Collection<String>) {
        moveNext(queueTrackIds)
        play(nextItemIndex)
    }

    fun onMoveTrackFinished(from: Int, to: Int) {
        player?.moveMediaItem(from, to)
    }

    fun play(index: Int? = null) {
        lastAction = LastAction.PLAY
        player?.run {
            _isLoading.value = true
            if (playbackState == Player.STATE_IDLE) prepare()
            if (index != null) seekTo(index, 0L)
            play()
        }
    }

    fun playOrPauseCurrent() {
        /** Plays/pauses the current item, if any. */
        player?.run {
            if (isPlaying) {
                lastAction = LastAction.PAUSE
                pause()
            } else this@PlayerRepository.play()
        }
    }

    fun removeFromQueue(queueTrackIds: Collection<String>) {
        launchOnMainThread {
            queueMutex.withLock {
                val indices = _queue.value.mapIndexedNotNull { index, combo ->
                    if (queueTrackIds.contains(combo.queueTrackId)) index else null
                }
                val intervals = indices.splitIntervals(descending = true)

                intervals.forEach { (fromIdx, toIdx) -> player?.removeMediaItems(fromIdx, toIdx) }
            }
        }
    }

    fun replace(combo: QueueTrackCombo) {
        clearQueue()
        replaceSignal.trySend(true)
        player?.addMediaItem(combo.copy(position = 0).toMediaItem())
    }

    fun replaceAndPlay(combo: QueueTrackCombo) {
        /** Clear queue, add track, play. */
        replace(combo)
        play(0)
    }

    fun seekBack() {
        player?.seekBack()
    }

    fun seekForward() {
        player?.seekForward()
    }

    fun seekToProgress(progress: Float) {
        player?.also { player ->
            val endPosition = _currentCombo.value?.track?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }
            if (endPosition != null) player.seekTo((endPosition * progress).toLong())
        }
    }

    fun skipTo(index: Int) {
        player?.also {
            if (it.mediaItemCount > index) {
                _isLoading.value = true
                play(index)
            }
        }
    }

    fun skipToNext() {
        player?.run {
            if (hasNextMediaItem()) {
                if (isPlaying) _isLoading.value = true
                if (playbackState == Player.STATE_IDLE) prepare()
                seekToNextMediaItem()
            }
        }
    }

    fun skipToPrevious() {
        player?.run {
            if (isPlaying) _isLoading.value = true
            if (playbackState == Player.STATE_IDLE) prepare()
            seekToPreviousMediaItem()
        }
    }

    fun skipToStartOrPrevious() {
        player?.run {
            if (currentPosition > 5000 || !hasPreviousMediaItem()) seekTo(0L)
            else skipToPrevious()
        }
    }

    fun stop() {
        lastAction = LastAction.STOP
        _isLoading.value = false
        player?.stop()
    }

    fun toggleRepeat() {
        player?.repeatMode = when (player?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        player?.run { shuffleModeEnabled = !shuffleModeEnabled }
    }

    fun updateTrack(combo: QueueTrackCombo) {
        player?.replaceMediaItem(combo.position, combo.toMediaItem())
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun findQueueTrackByMediaItem(mediaItem: MediaItem?): QueueTrackCombo? =
        mediaItem?.mediaId?.let { itemId -> _queue.value.find { it.queueTrackId == itemId } }

    private fun saveCurrentPosition() {
        player?.also {
            preferences.edit().putLong(PREF_CURRENT_TRACK_POSITION, it.currentPosition).apply()
        }
    }

    private fun saveQueueIndex() =
        player?.also { preferences.edit().putInt(PREF_QUEUE_INDEX, it.currentMediaItemIndex).apply() }

    private fun updateNextAndPreviousCombos() = player?.run {
        val nextMediaItem =
            if (mediaItemCount > currentMediaItemIndex + 1) getMediaItemAt(currentMediaItemIndex + 1)
            else null
        val previousMediaItem =
            if (currentMediaItemIndex > 0) getMediaItemAt(currentMediaItemIndex - 1)
            else null

        _nextCombo.value = findQueueTrackByMediaItem(nextMediaItem)
        _previousCombo.value = findQueueTrackByMediaItem(previousMediaItem)
    }

    private fun updateWidget() {
        launchOnIOThread { AppWidget().updateAll(context) }
    }


    /** OVERRIDDEN METHODS ********************************************************************************************/

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).also {
            if (it != _canGotoNext.value) _canGotoNext.value = it
        }
        availableCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).also {
            if (it != _canGotoPrevious.value) _canGotoPrevious.value = it
        }
        availableCommands.contains(Player.COMMAND_PLAY_PAUSE).also {
            if (it != _canPlay.value) _canPlay.value = it
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playbackState.value = when {
            isPlaying -> {
                if (currentTrackPlayStartTimestamp.value == null)
                    currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                _isLoading.value = false
                PlaybackState.PLAYING
            }
            player?.playbackState == Player.STATE_READY && player?.playWhenReady == false -> PlaybackState.PAUSED
            else -> PlaybackState.STOPPED
        }
        saveCurrentPosition()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Called when player's currently playing MediaItem changes.
        launchOnMainThread {
            queueMutex.withLock {
                saveCurrentPosition()
                saveQueueIndex()
                val combo = findQueueTrackByMediaItem(mediaItem)

                if (combo != _currentCombo.value) {
                    log("current track: ${combo?.track}, URI: ${combo?.uri}")
                    _currentCombo.value = combo
                    if (combo != null) playedQueueTrackIds.value += combo.queueTrackId
                    player?.currentPosition?.also {
                        _currentPositionMs.value = it
                    }
                    if (playbackState.value == PlaybackState.PLAYING)
                        currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                    else currentTrackPlayStartTimestamp.value = null
                    updateNextAndPreviousCombos()
                }
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        listeners.forEach { it.onPlayerError(error, _currentCombo.value, lastAction) }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        // Called when seekTo() has been run. We need this to be able to update current position even if the track is
        // not playing.
        if (newPosition.positionMs != oldPosition.positionMs) _currentPositionMs.value = newPosition.positionMs
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        _isRepeatEnabled.value = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> false
            else -> true
        }
        updateWidget()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        _isShuffleEnabled.value = shuffleModeEnabled
        updateWidget()
        updateNextAndPreviousCombos()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) launchOnMainThread {
            queueMutex.withLock {
                val queue = mutableListOf<QueueTrackCombo>()
                val queueTrackIds = mutableListOf<String>()

                if (timeline.windowCount > 0) {
                    val window = Timeline.Window()
                    for (idx in 0 until timeline.windowCount) {
                        timeline.getWindow(idx, window)
                        queueTrackIds.add(window.mediaItem.mediaId)
                        window.mediaItem.localConfiguration?.tag?.also { tag ->
                            if (tag is QueueTrackCombo) queue.add(tag)
                        }
                    }
                }

                val queueReindexed = queueTrackIds
                    .mapNotNull { id -> _queue.value.plus(queue).find { it.queueTrackId == id } }
                    .reindexed()

                if (queueReindexed != _queue.value) {
                    val newAndChanged = queueReindexed.filter { !_queue.value.containsWithPosition(it) }
                    val removed = _queue.value.filter { !queueTrackIds.contains(it.queueTrackId) }

                    log(Log.DEBUG, "onTimelineChanged: old queue=${_queue.value.map { it.track }}")
                    log(Log.DEBUG, "onTimelineChanged: new queue=${queueReindexed.map { it.track }}")

                    onIOThread {
                        queueDao.upsertQueueTracks(*newAndChanged.map { it.queueTrack }.toTypedArray())
                        if (removed.isNotEmpty())
                            queueDao.deleteQueueTracks(*removed.map { it.queueTrack.queueTrackId }.toTypedArray())
                    }
                    _queue.value = queueReindexed
                    saveCurrentPosition()
                    saveQueueIndex()
                    updateNextAndPreviousCombos()
                }
            }
        }
    }
}
