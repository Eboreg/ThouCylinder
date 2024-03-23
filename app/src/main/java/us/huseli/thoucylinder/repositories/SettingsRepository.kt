package us.huseli.thoucylinder.repositories

import android.content.Context
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
import us.huseli.thoucylinder.Constants.PREF_LIBRARY_RADIO_NOVELTY
import us.huseli.thoucylinder.Constants.PREF_LOCAL_MUSIC_URI
import us.huseli.thoucylinder.Constants.PREF_UMLAUTIFY
import us.huseli.thoucylinder.Constants.PREF_WELCOME_DIALOG_SHOWN
import us.huseli.thoucylinder.Umlautify
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _autoImportLocalMusic = MutableStateFlow(
        if (preferences.contains(PREF_AUTO_IMPORT_LOCAL_MUSIC))
            preferences.getBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, false)
        else null
    )
    private val _contentAreaSize = MutableStateFlow(DpSize.Zero)
    private val _innerPadding = MutableStateFlow(PaddingValues())
    private val _isWelcomeDialogShown = MutableStateFlow(preferences.getBoolean(PREF_WELCOME_DIALOG_SHOWN, false))
    private val _libraryRadioNovelty = MutableStateFlow(preferences.getFloat(PREF_LIBRARY_RADIO_NOVELTY, 0.5f))
    private val _localMusicUri = MutableStateFlow(preferences.getString(PREF_LOCAL_MUSIC_URI, null)?.toUri())
    private val _umlautify = MutableStateFlow(preferences.getBoolean(PREF_UMLAUTIFY, false))

    val autoImportLocalMusic: StateFlow<Boolean?> = _autoImportLocalMusic.asStateFlow()
    val contentAreaSize: StateFlow<DpSize> = _contentAreaSize.asStateFlow()
    val innerPadding: StateFlow<PaddingValues> = _innerPadding.asStateFlow()
    val isWelcomeDialogShown: StateFlow<Boolean> = _isWelcomeDialogShown.asStateFlow()
    val libraryRadioNovelty: StateFlow<Float> = _libraryRadioNovelty.asStateFlow()
    val localMusicUri: StateFlow<Uri?> = _localMusicUri.asStateFlow()
    val umlautify: StateFlow<Boolean> = _umlautify.asStateFlow()

    init {
        if (_umlautify.value) Umlautify.enabled = true
    }

    fun createAlbumDirectory(albumCombo: AbstractAlbumCombo): DocumentFile? =
        getLocalMusicDirectory()?.let { albumCombo.createDirectory(it, context) }

    fun getAlbumDirectory(albumCombo: AbstractAlbumCombo): DocumentFile? =
        getLocalMusicDirectory()?.let { albumCombo.getDirectory(it, context) }

    fun getLocalMusicDirectory(): DocumentFile? = _localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

    fun setAutoImportLocalMusic(value: Boolean) {
        _autoImportLocalMusic.value = value
        preferences.edit().putBoolean(PREF_AUTO_IMPORT_LOCAL_MUSIC, value).apply()
    }

    fun setContentAreaSize(value: DpSize) {
        _contentAreaSize.value = value
    }

    fun setInnerPadding(value: PaddingValues) {
        _innerPadding.value = value
    }

    fun setLibraryRadioNovelty(value: Float) {
        _libraryRadioNovelty.value = value
        preferences.edit().putFloat(PREF_LIBRARY_RADIO_NOVELTY, value).apply()
    }

    fun setLocalMusicUri(value: Uri?) {
        _localMusicUri.value = value
        preferences.edit().putString(PREF_LOCAL_MUSIC_URI, value?.toString()).apply()
    }

    fun setUmlautify(value: Boolean) {
        _umlautify.value = value
        Umlautify.enabled = value
        preferences.edit().putBoolean(PREF_UMLAUTIFY, value).apply()
    }

    fun setWelcomeDialogShown(value: Boolean) {
        preferences.edit().putBoolean(PREF_WELCOME_DIALOG_SHOWN, value).apply()
        _isWelcomeDialogShown.value = value
    }
}
