package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.album.Album
import us.huseli.thoucylinder.managers.Managers
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
open class DownloadsViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _isLoading = MutableStateFlow(true)

    val trackDownloadTasks: StateFlow<ImmutableList<TrackDownloadTask>> = managers.library.trackDownloadTasks
        .map { tasks -> tasks.sortedByDescending { it.started }.toImmutableList() }
        .onEach { _isLoading.value = false }
        .stateWhileSubscribed(persistentListOf())
    val isLoading = _isLoading.asStateFlow()
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri

    fun cancelAlbumDownload(albumId: String) = managers.library.cancelAlbumDownload(albumId)

    fun downloadAlbum(album: Album, onGotoAlbumClick: () -> Unit) {
        managers.library.downloadAlbum(
            albumId = album.albumId,
            onFinish = { result ->
                repos.message.onAlbumDownloadFinish(
                    album = album.toString(),
                    result = result,
                    onGotoAlbumClick = onGotoAlbumClick,
                )
            },
            onTrackError = { combo, throwable ->
                repos.message.onTrackDownloadError(track = combo.track.title, error = throwable)
            }
        )
    }

    fun downloadTrack(trackId: String) = managers.library.downloadTrack(trackId)
}
