package us.huseli.thoucylinder.managers

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.getFullBitmap
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder() {
    val albumArtAverageColor: StateFlow<Color?> = repos.player.currentCombo.map { combo ->
        combo?.let { repos.image.getTrackComboFullImageBitmap(it) }?.getAverageColor()?.copy(alpha = 0.3f)
    }.stateLazily()
    val canGotoNext = repos.player.canGotoNext
    val canGotoPrevious = repos.player.canGotoPrevious
    val canPlay = repos.player.canPlay.stateLazily(false)
    val currentBitmap: StateFlow<Bitmap?> = repos.player.currentCombo
        .map { combo ->
            combo?.album?.albumArt?.fullUri?.getFullBitmap(context, saveToCache = true)
                ?: combo?.track?.image?.fullUri?.getFullBitmap(context, saveToCache = true)
        }
        .distinctUntilChanged()
        .stateLazily()
    val currentTrackString: StateFlow<String?> = repos.player.currentCombo
        .map { combo -> combo?.let { listOfNotNull(it.artistString, it.track.title) }?.joinToString(" • ") }
        .distinctUntilChanged()
        .stateLazily()
    val isPlaying = repos.player.isPlaying.stateLazily(false)

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun seekBack() = repos.player.seekBack()

    fun seekForward() = repos.player.seekForward()

    fun skipToNext() = repos.player.skipToNext()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()
}
