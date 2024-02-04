package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.Repositories
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val autoImportLocalMusic: StateFlow<Boolean?> = repos.settings.autoImportLocalMusic
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri
    val lastFmUsername: StateFlow<String?> = repos.settings.lastFmUsername
    val lastFmScrobble: StateFlow<Boolean> = repos.settings.lastFmScrobble

    fun disableLastFmScrobble() = repos.settings.setLastFmScrobble(false)

    fun setAutoImportLocalMusic(value: Boolean) = repos.settings.setAutoImportLocalMusic(value)

    fun setLastFmUsername(value: String?) = repos.settings.setLastFmUsername(value)

    fun setLocalMusicUri(value: Uri?) = repos.settings.setLocalMusicUri(value)
}
