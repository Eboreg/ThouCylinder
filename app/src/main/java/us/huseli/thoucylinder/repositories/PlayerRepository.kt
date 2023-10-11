package us.huseli.thoucylinder.repositories

import android.content.Context
import android.util.Log
import androidx.media3.common.BuildConfig
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.QueueTrackPojo
import us.huseli.thoucylinder.dataclasses.TrackQueue
import us.huseli.thoucylinder.dataclasses.abstr.AbstractQueueTrack
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.toQueueTrackPojos
import javax.inject.Inject
import javax.inject.Singleton

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
            mutex.withLock {
                val pojos = queueDao.getQueue()
                val currentIndex =
                    preferences.getInt(PREF_QUEUE_INDEX, 0).takeIf { it < pojos.size && it > -1 } ?: 0

                if (pojos.isNotEmpty()) {
                    _queue.value = TrackQueue(pojos)
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

    /** Clear queue, add albums, play. */
    fun playAlbum(album: AlbumWithTracksPojo) = playAlbums(listOf(album))

    fun playAlbums(albums: List<AlbumWithTracksPojo>) = scope.launch {
        val pojos = albums.flatMap { album -> album.tracks.toQueueTrackPojos() }

        player.clearMediaItems()
        if (pojos.isNotEmpty()) {
            player.addMediaItems(pojos.map { it.toMediaItem() })
            player.seekTo(0, 0L)
            player.play()
        }
        saveQueue(TrackQueue(pojos))
    }

    fun play(pojo: QueueTrackPojo) = scope.launch {
        val index = _queue.value.indexOf(pojo)

        player.seekTo(index, 0L)
        player.play()
    }

    /** Plays/pauses the current item, if any. */
    fun playOrPauseCurrent() = scope.launch {
        if (player.isPlaying) player.pause()
        else player.play()
    }

    /** Add track to queue and play it. */
    fun playTrack(track: Track) = scope.launch {
        val index = player.currentMediaItemIndex + 1
        val pojo = track.toQueueTrackPojo(index)

        if (pojo != null) {
            player.addMediaItem(index, pojo.toMediaItem())
            player.seekTo(index, 0L)
            player.play()
            saveQueue(_queue.value.plus(pojo, index))
        }
    }

    fun removeFromQueue(pojos: List<AbstractQueueTrack>) {
        val indices = _queue.value.getIndices(pojos)

        indices.forEach { index ->
            player.removeMediaItem(index)
        }
        scope.launch {
            saveQueue(_queue.value.removeAt(indices))
        }
    }

    fun seekTo(positionMs: Long) = scope.launch {
        player.seekTo(positionMs)
    }

    fun skipToNext() = scope.launch {
        log("skipToNext: currentposition=${player.currentMediaItemIndex}, current item=${player.currentMediaItem}")
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun skipToPrevious() = scope.launch {
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
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

    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.i("PlayerRepository", message)
    }

    private suspend fun saveQueue(queue: TrackQueue) {
        mutex.withLock {
            _queue.value = queue
            queueDao.setQueue(queue.items)
        }
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
            else if (player.playbackState == Player.STATE_READY && !player.playWhenReady)
                PlaybackState.PAUSED
            else {
                PlaybackState.STOPPED
            }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Called when player's currently playing MediaItem changes.
        val reasonStr = when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "Playback has automatically transitioned to the next media item."
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "The current media item has changed because of a change in the playlist."
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "The media item has been repeated."
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "A seek to another media item has occurred."
            else -> "Blubb"
        }
        log("onMediaItemTransition: $reasonStr")
        saveQueueIndex(player.currentMediaItemIndex)
        scope.launch {
            val pojo = findQueueItemByMediaItem(mediaItem)
            if (pojo != _currentPojo.value) _currentPojo.value = pojo
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
}
