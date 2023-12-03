package us.huseli.thoucylinder.repositories

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.Constants.PREF_AUTO_IMPORT_LOCAL_MUSIC
import us.huseli.thoucylinder.Constants.PREF_MUSIC_DOWNLOAD_DIRECTORY
import us.huseli.thoucylinder.Constants.PREF_MUSIC_IMPORT_DIRECTORY
import us.huseli.thoucylinder.Constants.PREF_MUSIC_IMPORT_VOLUME
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _autoImportLocalMusic = MutableStateFlow(
        if (preferences.contains(PREF_AUTO_IMPORT_LOCAL_MUSIC))
            preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
        else null
    )
    private val _musicImportVolume = MutableStateFlow(preferences.getString(PREF_MUSIC_IMPORT_VOLUME, null))
    private val _musicImportDirectory = MutableStateFlow(
        preferences.getString(PREF_MUSIC_IMPORT_DIRECTORY, null) ?: Environment.DIRECTORY_MUSIC
    )
    private val _musicDownloadDirectory =
        MutableStateFlow(preferences.getString(PREF_MUSIC_DOWNLOAD_DIRECTORY, null) ?: Environment.DIRECTORY_MUSIC)

    val autoImportLocalMusic: StateFlow<Boolean?> = _autoImportLocalMusic.asStateFlow()
    val musicImportDirectory: StateFlow<String> = _musicImportDirectory.asStateFlow()
    val musicDownloadDirectory: StateFlow<String> = _musicDownloadDirectory.asStateFlow()
    val musicImportVolume: StateFlow<String?> = _musicImportVolume.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun setAutoImportLocalMusic(value: Boolean) {
        preferences.edit().putBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, value).apply()
    }

    fun setMusicImportVolume(value: String) {
        preferences.edit().putString(PREF_MUSIC_IMPORT_VOLUME, value).apply()
    }

    fun setMusicImportDirectory(value: String) {
        preferences.edit().putString(PREF_MUSIC_IMPORT_DIRECTORY, value).apply()
    }

    fun setMusicDownloadDirectory(value: String) {
        preferences.edit().putString(PREF_MUSIC_DOWNLOAD_DIRECTORY, value).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_AUTO_IMPORT_LOCAL_MUSIC -> _autoImportLocalMusic.value = preferences.getBoolean(key, false)
            PREF_MUSIC_IMPORT_DIRECTORY -> preferences.getString(key, null)?.also { _musicImportDirectory.value = it }
            PREF_MUSIC_DOWNLOAD_DIRECTORY ->
                preferences.getString(key, null)?.also { _musicDownloadDirectory.value = it }
            PREF_MUSIC_IMPORT_VOLUME -> _musicImportVolume.value = preferences.getString(key, null)
        }
    }
}