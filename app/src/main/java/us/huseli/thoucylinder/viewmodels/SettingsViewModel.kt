package us.huseli.thoucylinder.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val autoImportLocalMusic = repos.settings.autoImportLocalMusic
    val musicImportDirectory = repos.settings.musicImportDirectory
    val musicDownloadDirectory = repos.settings.musicDownloadDirectory
    val musicImportVolume = repos.settings.musicImportVolume

    fun setAutoImportLocalMusic(value: Boolean) = repos.settings.setAutoImportLocalMusic(value)

    fun setMusicImportDirectory(value: String) = repos.settings.setMusicImportDirectory(value)

    fun setMusicImportVolume(value: String) = repos.settings.setMusicImportVolume(value)

    fun setMusicDownloadDirectory(value: String) = repos.settings.setMusicDownloadDirectory(value)
}
