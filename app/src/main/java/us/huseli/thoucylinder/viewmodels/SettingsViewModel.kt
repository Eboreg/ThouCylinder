package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.enums.Region
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    val autoImportLocalMusic: StateFlow<Boolean?> = repos.settings.autoImportLocalMusic
    val lastFmIsAuthenticated: Flow<Boolean> = repos.lastFm.isAuthenticated
    val lastFmScrobble: StateFlow<Boolean> = repos.lastFm.scrobble
    val lastFmUsername: StateFlow<String?> = repos.lastFm.username
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri
    val region: StateFlow<Region> = repos.settings.region
    val umlautify: StateFlow<Boolean> = repos.settings.umlautify

    fun disableLastFmScrobble() = repos.lastFm.setScrobble(false)

    fun enableLastFmScrobble() = repos.lastFm.setScrobble(true)

    fun importNewLocalAlbums() = managers.library.importNewLocalAlbumsAsync()

    fun setAutoImportLocalMusic(value: Boolean) = repos.settings.setAutoImportLocalMusic(value)

    fun setLastFmUsername(value: String?) = repos.lastFm.setUsername(value)

    fun setLocalMusicUri(value: Uri?) = repos.settings.setLocalMusicUri(value)

    fun setRegion(value: Region) = repos.settings.setRegion(value)

    fun setUmlautify(value: Boolean) = repos.settings.setUmlautify(value)

    fun unhideLocalAlbums(onFinish: () -> Unit) {
        launchOnIOThread {
            repos.album.unhideLocalAlbums()
            onFinish()
        }
    }
}
