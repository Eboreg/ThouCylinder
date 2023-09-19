package us.huseli.thoucylinder.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _currentUri = MutableStateFlow<Uri?>(null)
    private val _isPlaying = MutableStateFlow(false)

    val currentUri = _currentUri.asStateFlow()
    val isPlaying = _isPlaying.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    fun playOrPause(uri: Uri) = scope.launch {
        Log.i("PlayerRepository", "playOrPause: uri=$uri")
        if (uri == _currentUri.value) {
            if (_isPlaying.value) exoPlayer.pause()
            else exoPlayer.play()
        } else {
            exoPlayer.stop()
            _currentUri.value = null
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.play()
            _currentUri.value = uri
        }
    }
}
