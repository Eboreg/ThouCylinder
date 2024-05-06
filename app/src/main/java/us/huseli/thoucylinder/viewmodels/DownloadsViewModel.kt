package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
open class DownloadsViewModel @Inject constructor(
    repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    val trackDownloadTasks: StateFlow<ImmutableList<TrackDownloadTask>> = managers.library.trackDownloadTasks
        .map { tasks -> tasks.sortedByDescending { it.started }.toImmutableList() }
        .stateLazily(persistentListOf())
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri

    fun cancelAlbumDownload(albumId: String) = managers.library.cancelAlbumDownload(albumId)

    fun downloadAlbum(
        albumId: String,
        onFinish: (hasErrors: Boolean) -> Unit,
        onTrackError: (TrackCombo, Throwable) -> Unit,
    ) = managers.library.downloadAlbum(albumId = albumId, onFinish = onFinish, onTrackError = onTrackError)

    fun downloadTrack(trackId: String) = managers.library.downloadTrack(trackId)

    suspend fun getThumbnail(uri: Uri?): ImageBitmap? = managers.image.getThumbnailImageBitmap(uri)
}
