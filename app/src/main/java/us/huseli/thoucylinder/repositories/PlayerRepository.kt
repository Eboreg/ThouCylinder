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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.thoucylinder.Constants.PREF_CURRENT_TRACK_POSITION
import us.huseli.thoucylinder.Constants.PREF_QUEUE_INDEX
import us.huseli.thoucylinder.PlaybackService
import us.huseli.thoucylinder.PlayerRepositoryListener
import us.huseli.thoucylinder.database.QueueDao
import us.huseli.thoucylinder.dataclasses.combos.QueueTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.containsWithPosition
import us.huseli.thoucylinder.dataclasses.combos.reindexed
import us.huseli.thoucylinder.dataclasses.combos.toMediaItems
import us.huseli.thoucylinder.widget.AppWidget
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.time.DurationUnit

@Singleton
@MainThread
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueDao: QueueDao,
) : Player.Listener {
    enum class PlaybackState { STOPPED, PLAYING, PAUSED }
    enum class LastAction { PLAY, STOP, PAUSE }

    private val listeners = mutableListOf<PlayerRepositoryListener>()
    private val queueMutex = Mutex()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentTrackPlayTimeJob: Job? = null
    private var onPlayerReady: (MediaController) -> Unit = {}
    private var player: MediaController? = null
    private var positionSaveJob: Job? = null
    private var positionUpdateJob: Job? = null

    private val _canGotoNext = MutableStateFlow(player?.hasNextMediaItem() ?: false)
    private val _canPlay = MutableStateFlow(player?.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) ?: false)
    private val _currentCombo = MutableStateFlow<QueueTrackCombo?>(null)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _currentTrackPlayStartTimestamp = MutableStateFlow<Long?>(null)
    private val _currentTrackPlayTime = MutableStateFlow(0) // seconds
    private val _isCurrentTrackHalfPlayedReported = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)
    private val _isRepeatEnabled = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())
    private var _lastAction = LastAction.STOP

    val canGotoNext: StateFlow<Boolean> = _canGotoNext.asStateFlow()
    val canPlay = combine(_canPlay, _isLoading) { canPlay, isLoading -> canPlay && !isLoading }.distinctUntilChanged()
    val currentCombo: StateFlow<QueueTrackCombo?> = _currentCombo.asStateFlow()
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val isPlaying: Flow<Boolean> = _playbackState.map { it == PlaybackState.PLAYING }.distinctUntilChanged()
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()
    val queue: StateFlow<List<QueueTrackCombo>> = _queue.asStateFlow()

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

        scope.launch {
            /**
             * Only use queue data from the database in order to bootstrap the queue on app start. Thereafter, data
             * flows unidirectionally _from_ ExoPlayer _to_ the database, basically making Exoplayer instance the
             * single source of truth for queue contents and playback status etc, with some stateflows acting as cache.
             */
            queueMutex.withLock {
                val combos = queueDao.getQueue()
                val currentIndex =
                    preferences.getInt(PREF_QUEUE_INDEX, 0).takeIf { it < combos.size && it > -1 } ?: 0
                val currentTrackPosition = preferences.getLong(PREF_CURRENT_TRACK_POSITION, 0)

                _queue.value = combos

                if (combos.isNotEmpty()) {
                    val func = { player: MediaController ->
                        player.addMediaItems(combos.map { it.toMediaItem() })
                        player.seekTo(currentIndex, currentTrackPosition)
                    }

                    player?.also(func) ?: run { onPlayerReady = func }
                }
            }
        }

        scope.launch {
            /**
             * However, continuously monitor _track_ data from the database, in case URIs change because a track has
             * been downloaded or locally deleted.
             */
            queueDao.flowTracksInQueue().distinctUntilChanged().collect { tracks ->
                queueMutex.withLock {
                    val uriMap =
                        _queue.value.associateWith { combo -> tracks.find { it.trackId == combo.track.trackId }?.playUri }

                    // Handle updated URIs:
                    uriMap.filterValuesNotNull().forEach { (combo, uri) ->
                        if (uri != combo.uri) updateTrack(combo.copy(uri = uri))
                    }
                }
            }
        }

        scope.launch {
            _playbackState.collect { state ->
                if (state == PlaybackState.PLAYING) {
                    positionUpdateJob = launch {
                        while (true) {
                            player?.currentPosition?.also { _currentPositionMs.value = it }
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
                                val combo = _currentCombo.value
                                val duration = combo?.track?.duration

                                if (combo != null && duration != null) {
                                    // Scrobble when the track has been played for at least half its duration, or for 4
                                    // minutes (whichever occurs earlier).
                                    // https://www.last.fm/api/scrobbling#when-is-a-scrobble-a-scrobble
                                    val threshold = min(duration.inWholeSeconds / 2, 240L)

                                    if (_currentTrackPlayTime.value >= threshold) {
                                        _currentTrackPlayStartTimestamp.value?.also { startTimestamp ->
                                            listeners.forEach { it.onHalfTrackPlayed(combo, startTimestamp) }
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

                listeners.forEach { it.onPlaybackChange(_currentCombo.value, state) }
                updateWidget()
            }
        }

        scope.launch {
            _currentCombo.collect { combo ->
                _currentTrackPlayTime.value = 0
                _isCurrentTrackHalfPlayedReported.value = false
                listeners.forEach { it.onPlaybackChange(combo, _playbackState.value) }
                updateWidget()
            }
        }
    }

    fun addListener(listener: PlayerRepositoryListener) = listeners.add(listener)

    fun insertNext(combos: List<QueueTrackCombo>) {
        if (combos.isNotEmpty()) player?.addMediaItems(nextItemIndex, combos.toMediaItems())
    }

    fun insertNextAndPlay(combo: QueueTrackCombo) {
        player?.addMediaItem(nextItemIndex, combo.toMediaItem())
        player?.seekTo(nextItemIndex, 0L)
        play()
    }

    fun moveNext(combos: List<QueueTrackCombo>) {
        removeFromQueue(combos)
        player?.addMediaItems(nextItemIndex, combos.map { it.toMediaItem() })
    }

    fun moveNextAndPlay(combos: List<QueueTrackCombo>) {
        moveNext(combos)
        player?.seekTo(nextItemIndex, 0L)
        play()
    }

    fun onMoveTrackFinished(from: Int, to: Int) = player?.moveMediaItem(from, to)

    fun play(index: Int? = null) {
        _lastAction = LastAction.PLAY
        player?.also {
            _isLoading.value = true
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            if (index != null) it.seekTo(index, 0L)
            it.play()
        }
    }

    fun playOrPauseCurrent() {
        /** Plays/pauses the current item, if any. */
        player?.also {
            if (it.isPlaying) {
                _lastAction = LastAction.PAUSE
                it.pause()
            } else play()
        }
    }

    fun removeFromQueue(combos: List<QueueTrackCombo>) = scope.launch {
        queueMutex.withLock {
            val ids = combos.map { it.queueTrackId }
            val indices =
                _queue.value.mapIndexedNotNull { index, combo -> if (ids.contains(combo.queueTrackId)) index else null }

            indices.sortedDescending().forEach { index ->
                player?.removeMediaItem(index)
            }
        }
    }

    fun replaceAndPlay(combos: List<QueueTrackCombo>, startIndex: Int? = 0) {
        /** Clear queue, add tracks, play. */
        player?.clearMediaItems()
        if (combos.isNotEmpty()) {
            player?.addMediaItems(combos.map { it.toMediaItem() })
            player?.seekTo(max(startIndex ?: 0, 0), 0L)
            play()
        }
    }

    fun seekToProgress(progress: Float) = player?.also { player ->
        val endPosition = _currentCombo.value?.track?.duration?.toLong(DurationUnit.MILLISECONDS)?.takeIf { it > 0 }
        if (endPosition != null) player.seekTo((endPosition * progress).toLong())
    }

    fun skipTo(index: Int) {
        player?.also {
            if (it.mediaItemCount > index) {
                _isLoading.value = true
                it.seekTo(index, 0L)
                play()
            }
        }
    }

    fun skipToNext() {
        player?.also {
            if (it.hasNextMediaItem()) {
                if (it.isPlaying) _isLoading.value = true
                if (it.playbackState == Player.STATE_IDLE) it.prepare()
                it.seekToNextMediaItem()
            }
        }
    }

    fun skipToPrevious() {
        player?.also {
            if (it.isPlaying) _isLoading.value = true
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.seekToPreviousMediaItem()
        }
    }

    fun skipToStartOrPrevious() {
        player?.also {
            if (it.currentPosition > 5000 || !it.hasPreviousMediaItem()) it.seekTo(0L)
            else skipToPrevious()
        }
    }

    fun toggleRepeat() {
        player?.repeatMode = when (player?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        player?.also { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun updateTrack(combo: QueueTrackCombo) {
        player?.also {
            it.removeMediaItem(combo.position)
            it.addMediaItem(combo.position, combo.toMediaItem())
            if (combo.position <= it.currentMediaItemIndex && _playbackState.value != PlaybackState.PLAYING)
                it.seekTo(it.currentMediaItemIndex - 1, 0L)
        }
    }


    /** PRIVATE METHODS ******************************************************/
    private fun findQueueItemByMediaItem(mediaItem: MediaItem?): QueueTrackCombo? =
        mediaItem?.mediaId?.let { itemId -> _queue.value.find { it.queueTrackId.toString() == itemId } }

    fun getNextTrack(): QueueTrackCombo? = player?.let {
        val nextMediaItem =
            if (it.mediaItemCount > it.currentMediaItemIndex + 1)
                it.getMediaItemAt(it.currentMediaItemIndex + 1)
            else null

        findQueueItemByMediaItem(nextMediaItem)
    }

    fun getPreviousTrack(): QueueTrackCombo? = player?.let {
        val previousMediaItem =
            if (it.currentMediaItemIndex > 0) it.getMediaItemAt(it.currentMediaItemIndex - 1)
            else null

        findQueueItemByMediaItem(previousMediaItem)
    }

    private fun saveCurrentPosition() =
        player?.also { preferences.edit().putLong(PREF_CURRENT_TRACK_POSITION, it.currentPosition).apply() }

    private fun saveQueueIndex() =
        player?.also { preferences.edit().putInt(PREF_QUEUE_INDEX, it.currentMediaItemIndex).apply() }

    private fun updateWidget() = scope.launch(Dispatchers.IO) { AppWidget().updateAll(context) }


    /** OVERRIDDEN METHODS ***************************************************/
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).also {
            if (it != _canGotoNext.value) _canGotoNext.value = it
        }
        availableCommands.contains(Player.COMMAND_PLAY_PAUSE).also {
            if (it != _canPlay.value) _canPlay.value = it
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) Log.i(javaClass.simpleName, "Playing: ${player?.currentMediaItem?.localConfiguration?.uri}")
        _playbackState.value = when {
            isPlaying -> {
                if (_currentTrackPlayStartTimestamp.value == null)
                    _currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
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
        scope.launch {
            queueMutex.withLock {
                saveCurrentPosition()
                saveQueueIndex()
                val combo = findQueueItemByMediaItem(mediaItem)
                if (combo != _currentCombo.value) {
                    Log.i("PlayerRepository", "current track URI: ${combo?.uri}")
                    _currentCombo.value = combo
                    player?.currentPosition?.also { _currentPositionMs.value = it }
                    if (_playbackState.value == PlaybackState.PLAYING)
                        _currentTrackPlayStartTimestamp.value = System.currentTimeMillis() / 1000
                    else _currentTrackPlayStartTimestamp.value = null
                }
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        listeners.forEach { it.onPlayerError(error, _currentCombo.value, _lastAction) }
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
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) scope.launch {
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
                    .mapNotNull { id -> _queue.value.plus(queue).find { it.queueTrackId.toString() == id } }
                    .reindexed()
                val newAndChanged = queueReindexed.filter { !_queue.value.containsWithPosition(it) }
                val removed = _queue.value.filter { !queueTrackIds.contains(it.queueTrackId.toString()) }
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
}
