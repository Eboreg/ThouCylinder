package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.thoucylinder.repositories.YoutubeRepository

abstract class BaseDownloadViewModel : ViewModel() {
    protected val _downloadProgress = MutableStateFlow(0.0)
    protected val _downloadStatus = MutableStateFlow(YoutubeRepository.DownloadStatus())

    val downloadProgress = _downloadProgress.asStateFlow()
    val downloadStatus = _downloadStatus.asStateFlow()
}
