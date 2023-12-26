package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
import us.huseli.thoucylinder.Constants.PREF_MUSIC_DOWNLOAD_URI
import us.huseli.thoucylinder.Constants.PREF_MUSIC_IMPORT_URI
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
    private val _musicDownloadUri = MutableStateFlow(preferences.getString(PREF_MUSIC_DOWNLOAD_URI, null)?.toUri())
    private val _musicImportUri = MutableStateFlow(preferences.getString(PREF_MUSIC_IMPORT_URI, null)?.toUri())
    private val _lastFmUsername = MutableStateFlow(preferences.getString(PREF_LASTFM_USERNAME, null))
    private val _lastFmScrobble = MutableStateFlow(preferences.getBoolean(PREF_LASTFM_SCROBBLE, false))

    val autoImportLocalMusic: StateFlow<Boolean?> = _autoImportLocalMusic.asStateFlow()
    val musicDownloadUri: StateFlow<Uri?> = _musicDownloadUri.asStateFlow()
    val musicImportUri: StateFlow<Uri?> = _musicImportUri.asStateFlow()
    val lastFmUsername: StateFlow<String?> = _lastFmUsername.asStateFlow()
    val lastFmScrobble: StateFlow<Boolean> = _lastFmScrobble.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun getMusicDownloadDocumentFile(): DocumentFile? =
        _musicDownloadUri.value?.let { DocumentFile.fromTreeUri(context, it) }

    fun getMusicImportDocumentFile(): DocumentFile? =
        _musicImportUri.value?.let { DocumentFile.fromTreeUri(context, it) }

    fun setAutoImportLocalMusic(value: Boolean) {
        preferences.edit().putBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, value).apply()
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

    fun setMusicDownloadUri(value: Uri?) {
        preferences.edit().putString(PREF_MUSIC_DOWNLOAD_URI, value?.toString()).apply()
    }

    fun setMusicImportUri(value: Uri) {
        preferences.edit().putString(PREF_MUSIC_IMPORT_URI, value.toString()).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_AUTO_IMPORT_LOCAL_MUSIC -> _autoImportLocalMusic.value = preferences.getBoolean(key, false)
            PREF_MUSIC_DOWNLOAD_URI -> _musicDownloadUri.value = preferences.getString(key, null)?.toUri()
            PREF_MUSIC_IMPORT_URI -> _musicImportUri.value = preferences.getString(key, null)?.toUri()
            PREF_LASTFM_USERNAME -> _lastFmUsername.value = preferences.getString(key, null)
            PREF_LASTFM_SCROBBLE -> _lastFmScrobble.value = preferences.getBoolean(PREF_LASTFM_SCROBBLE, false)
        }
    }
}
