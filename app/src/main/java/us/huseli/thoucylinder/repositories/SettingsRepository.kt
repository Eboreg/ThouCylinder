package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.DpSize
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.Constants.PREF_AUTO_IMPORT_LOCAL_MUSIC
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SCROBBLE
import us.huseli.thoucylinder.Constants.PREF_LASTFM_SESSION_KEY
import us.huseli.thoucylinder.Constants.PREF_LASTFM_USERNAME
import us.huseli.thoucylinder.Constants.PREF_LOCAL_MUSIC_URI
import us.huseli.thoucylinder.Constants.PREF_WELCOME_DIALOG_SHOWN
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _autoImportLocalMusic = MutableStateFlow(
        if (preferences.contains(PREF_AUTO_IMPORT_LOCAL_MUSIC))
            preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
        else null
    )
    private val _localMusicUri = MutableStateFlow(preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri())
    private val _lastFmUsername = MutableStateFlow(preferences.getString(PREF_LASTFM_USERNAME, null))
    private val _lastFmScrobble = MutableStateFlow(preferences.getBoolean(PREF_LASTFM_SCROBBLE, false))
    private val _isWelcomeDialogShown = MutableStateFlow(preferences.getBoolean(PREF_WELCOME_DIALOG_SHOWN, false))
    private val _innerPadding = MutableStateFlow(PaddingValues())
    private val _contentAreaSize = MutableStateFlow(DpSize.Zero)

    val autoImportLocalMusic: StateFlow<Boolean?> = _autoImportLocalMusic.asStateFlow()
    val localMusicUri: StateFlow<Uri?> = _localMusicUri.asStateFlow()
    val lastFmUsername: StateFlow<String?> = _lastFmUsername.asStateFlow()
    val lastFmScrobble: StateFlow<Boolean> = _lastFmScrobble.asStateFlow()
    val isWelcomeDialogShown: StateFlow<Boolean> = _isWelcomeDialogShown.asStateFlow()
    val innerPadding = _innerPadding.asStateFlow()
    val contentAreaSize = _contentAreaSize.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun getLocalMusicDirectory(): DocumentFile? =
        _localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

    fun setAutoImportLocalMusic(value: Boolean) {
        preferences.edit().putBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, value).apply()
    }

    fun setContentAreaSize(value: DpSize) {
        _contentAreaSize.value = value
    }

    fun setInnerPadding(value: PaddingValues) {
        _innerPadding.value = value
    }

    fun setLastFmScrobble(value: Boolean) {
        preferences.edit().putBoolean(PREF_LASTFM_SCROBBLE, value).apply()
    }

    fun setLastFmSessionKey(value: String?) {
        preferences.edit().putString(PREF_LASTFM_SESSION_KEY, value).apply()
    }

    fun setLastFmUsername(value: String?) {
        preferences.edit().putString(PREF_LASTFM_USERNAME, value).apply()
    }

    fun setLocalMusicUri(value: Uri?) {
        preferences.edit().putString(PREF_LOCAL_MUSIC_URI, value?.toString()).apply()
    }

    fun setWelcomeDialogShown(value: Boolean) {
        preferences.edit().putBoolean(PREF_WELCOME_DIALOG_SHOWN, value).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_AUTO_IMPORT_LOCAL_MUSIC -> _autoImportLocalMusic.value = preferences.getBoolean(key, false)
            PREF_LOCAL_MUSIC_URI -> _localMusicUri.value = preferences.getString(key, null)?.toUri()
            PREF_LASTFM_USERNAME -> _lastFmUsername.value = preferences.getString(key, null)
            PREF_LASTFM_SCROBBLE -> _lastFmScrobble.value = preferences.getBoolean(key, false)
            PREF_WELCOME_DIALOG_SHOWN -> _isWelcomeDialogShown.value = preferences.getBoolean(key, false)
        }
    }
}
