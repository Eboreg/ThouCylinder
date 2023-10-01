package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(@ApplicationContext private val context: Context) {
    enum class PlaybackState { STOPPED, PLAYING, PAUSED }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _currentUri = MutableStateFlow<Uri?>(null)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)

    val currentUri = _currentUri.asStateFlow()
    val playingUri: Flow<Uri?> = combine(_currentUri, _playbackState) { currentUri, state ->
        if (state == PlaybackState.PLAYING) currentUri else null
    }
    val playbackState = _playbackState.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value =
                    if (isPlaying) PlaybackState.PLAYING
                    else if (exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.playWhenReady)
                        PlaybackState.PAUSED
                    else {
                        _currentUri.value = null
                        PlaybackState.STOPPED
                    }
            }
        })
    }

    fun playOrPause(uri: Uri) = scope.launch {
        if (uri == _currentUri.value) {
            if (_playbackState.value == PlaybackState.PLAYING) exoPlayer.pause()
            else exoPlayer.play()
        } else {
            exoPlayer.stop()
            _currentUri.value = null
            _playbackState.value = PlaybackState.STOPPED
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.play()
            _currentUri.value = uri
        }
    }
}
