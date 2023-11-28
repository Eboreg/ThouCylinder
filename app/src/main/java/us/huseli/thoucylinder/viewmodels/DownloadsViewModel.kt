package us.huseli.thoucylinder.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.repositories.Repositories
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
open class DownloadsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val trackDownloadTasks = repos.trackDownloadPool.tasks

    fun cancelAlbumDownload(albumId: UUID) {
        repos.youtube.albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    fun downloadAndSaveAlbum(
        album: Album,
        onTrackDownloadError: (Track, Throwable) -> Unit,
        onTrackDownloadFinish: (Track) -> Unit = {},
    ) = viewModelScope.launch {
        repos.room.getAlbumWithTracks(album.albumId)?.also {
            downloadAndSaveAlbumPojo(it, onTrackDownloadError, onTrackDownloadFinish)
        }
    }

    fun downloadAndSaveAlbumPojo(
        pojo: AlbumWithTracksPojo,
        onTrackDownloadError: (Track, Throwable) -> Unit = { _, _ -> },
        onTrackDownloadFinish: (Track) -> Unit = {},
    ) = viewModelScope.launch {
        val tracks = pojo.tracks.filter { !it.isDownloaded }.map { ensureTrackMetadata(it, commit = false) }
        val trackTasks = tracks.map { track ->
            repos.trackDownloadPool.addTask(
                track = track,
                repos = repos,
                albumPojo = pojo,
                onError = { onTrackDownloadError(track, it) },
                onFinish = onTrackDownloadFinish,
            )
        }
        val task = AlbumDownloadTask(pojo.album, trackTasks)

        repos.youtube.addAlbumDownloadTask(task)
        if (!pojo.album.isLocal) repos.room.saveAlbum(pojo.album.copy(isLocal = true))
    }

    fun downloadTrack(track: Track, albumPojo: AbstractAlbumPojo? = null) = viewModelScope.launch {
        repos.trackDownloadPool.addTask(
            track = ensureTrackMetadata(track, commit = false),
            albumPojo = albumPojo,
            repos = repos,
        )
    }
}
