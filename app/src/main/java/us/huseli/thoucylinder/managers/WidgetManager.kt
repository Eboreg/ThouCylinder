package us.huseli.thoucylinder.managers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.updateAll
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.AbstractScopeHolder
import us.huseli.thoucylinder.Constants.PREF_WIDGET_BUTTONS
import us.huseli.thoucylinder.getAverageColor
import us.huseli.thoucylinder.getFullBitmap
import us.huseli.thoucylinder.repositories.Repositories
import us.huseli.thoucylinder.widget.AppWidget
import us.huseli.thoucylinder.widget.WidgetButton
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _buttons = MutableStateFlow(
        WidgetButton.fromNames(
            preferences.getStringSet(PREF_WIDGET_BUTTONS, WidgetButton.defaultNames) ?: WidgetButton.defaultNames
        )
    )

    val albumArtAverageColor: StateFlow<Color?> = repos.player.currentCombo.map { combo ->
        combo?.let { repos.image.getTrackComboFullImageBitmap(it) }?.getAverageColor()?.copy(alpha = 0.3f)
    }.stateWhileSubscribed()
    val buttons = _buttons.asStateFlow()
    val canGotoNext = repos.player.canGotoNext
    val canGotoPrevious = repos.player.canGotoPrevious
    val canPlay = repos.player.canPlay.stateWhileSubscribed(false)
    val currentBitmap: StateFlow<Bitmap?> = repos.player.currentCombo
        .map { combo ->
            combo?.album?.albumArt?.fullUri?.getFullBitmap(context, saveToCache = true)
                ?: combo?.track?.image?.fullUri?.getFullBitmap(context, saveToCache = true)
        }
        .distinctUntilChanged()
        .stateWhileSubscribed()
    val currentTrackString: StateFlow<String?> = repos.player.currentCombo
        .map { combo -> combo?.let { listOfNotNull(it.artistString, it.track.title) }?.joinToString(" • ") }
        .distinctUntilChanged()
        .stateWhileSubscribed()
    val isPlaying = repos.player.isPlaying.stateWhileSubscribed(false)
    val isRepeatEnabled = repos.player.isRepeatEnabled
    val isShuffleEnabled = repos.player.isShuffleEnabled

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun seekBack() = repos.player.seekBack()

    fun seekForward() = repos.player.seekForward()

    fun skipToNext() = repos.player.skipToNext()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleShuffle() = repos.player.toggleShuffle()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_WIDGET_BUTTONS) {
            _buttons.value = WidgetButton.fromNames(
                preferences.getStringSet(PREF_WIDGET_BUTTONS, WidgetButton.defaultNames) ?: WidgetButton.defaultNames
            )
            launchOnIOThread { AppWidget().updateAll(context) }
        }
    }
}
