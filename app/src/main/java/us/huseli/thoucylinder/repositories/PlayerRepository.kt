package us.huseli.thoucylinder.repositories

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.dataclasses.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(@ApplicationContext private val context: Context) {
    enum class PlaybackState { STOPPED, PLAYING, PAUSED }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionPollJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)

    val currentPositionMs = _currentPositionMs.asStateFlow()
    val currentTrack = _currentTrack.asStateFlow()
    val playingTrack = combine(_currentTrack, _playbackState) { track, state ->
        if (state == PlaybackState.PLAYING) track else null
    }.distinctUntilChanged()
    val playbackState = _playbackState.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value =
                    if (isPlaying) PlaybackState.PLAYING
                    else if (exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.playWhenReady)
                        PlaybackState.PAUSED
                    else {
                        _currentTrack.value = null
                        PlaybackState.STOPPED
                    }
            }
        })

        scope.launch {
            _playbackState.collect { state ->
                if (state == PlaybackState.PLAYING) {
                    positionPollJob = launch {
                        while (true) {
                            _currentPositionMs.value = exoPlayer.currentPosition
                            delay(200)
                        }
                    }
                } else {
                    positionPollJob?.cancel()
                    positionPollJob = null
                }
            }
        }
    }

    fun playOrPause(track: Track) = scope.launch {
        track.playUri?.also { uri ->
            Log.i("PlayerRepository", "playOrPause: uri=$uri")
            if (track == _currentTrack.value) {
                if (_playbackState.value == PlaybackState.PLAYING) exoPlayer.pause()
                else exoPlayer.play()
            } else {
                exoPlayer.stop()
                _currentTrack.value = null
                _playbackState.value = PlaybackState.STOPPED
                exoPlayer.setMediaItem(MediaItem.fromUri(uri))
                exoPlayer.prepare()
                exoPlayer.play()
                _currentTrack.value = track
            }
        }
    }
}
