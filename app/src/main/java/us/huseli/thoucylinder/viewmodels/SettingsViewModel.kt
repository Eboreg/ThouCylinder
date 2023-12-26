package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.Repositories
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val autoImportLocalMusic: StateFlow<Boolean?> = repos.settings.autoImportLocalMusic
    val musicDownloadUri: StateFlow<Uri?> = repos.settings.musicDownloadUri
    val musicImportUri: StateFlow<Uri?> = repos.settings.musicImportUri
    val lastFmUsername: StateFlow<String?> = repos.settings.lastFmUsername
    val lastFmScrobble: StateFlow<Boolean> = repos.settings.lastFmScrobble

    fun setAutoImportLocalMusic(value: Boolean) = repos.settings.setAutoImportLocalMusic(value)

    fun setLastFmScobble(value: Boolean) = repos.settings.setLastFmScrobble(value)

    fun setLastFmUsername(value: String?) = repos.settings.setLastFmUsername(value)

    fun setMusicDownloadUri(value: Uri?) = repos.settings.setMusicDownloadUri(value)

    fun setMusicImportUri(value: Uri) = repos.settings.setMusicImportUri(value)
}
