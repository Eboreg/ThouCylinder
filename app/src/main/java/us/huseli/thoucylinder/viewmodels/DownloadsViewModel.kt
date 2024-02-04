package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.entities.Track
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
open class DownloadsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val trackDownloadTasks: Flow<List<TrackDownloadTask>> = repos.download.tasks
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri

    fun cancelAlbumDownload(albumId: UUID) {
        repos.youtube.albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    fun downloadAlbum(
        albumId: UUID,
        context: Context,
        onFinish: (hasErrors: Boolean) -> Unit = {},
        onTrackError: (Track, Throwable) -> Unit = { _, _ -> },
        onTrackFinish: (Track) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.getAlbumWithTracks(albumId)?.also { pojo ->
            repos.localMedia.createAlbumDocumentFile(pojo.album)?.also { dirDocumentFile ->
                val tracks = pojo.tracks.filter { !it.isDownloaded }
                    .map { ensureTrackMetadata(it, commit = false) }
                val trackTasks = tracks.map { track ->
                    repos.download.downloadTrack(
                        track = track,
                        repos = repos,
                        dirDocumentFile = dirDocumentFile,
                        albumPojo = pojo,
                        onError = { onTrackError(track, it) },
                        onFinish = onTrackFinish,
                    )
                }
                val albumArt = pojo.getOrCreateAlbumArt(context)

                pojo.album.albumArt?.saveToAlbumDirectory(context, dirDocumentFile)
                albumArt?.saveToAlbumDirectory(context, dirDocumentFile)
                repos.youtube.addAlbumDownloadTask(AlbumDownloadTask(pojo.album, trackTasks, onFinish))
                repos.album.saveAlbumPojo(
                    pojo.copy(
                        album = pojo.album.copy(
                            isLocal = true,
                            isInLibrary = true,
                            albumArt = albumArt,
                        )
                    )
                )
            }
        }
    }

    fun downloadTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        val dirDocumentFile = repos.settings.getLocalMusicDocumentFile()

        if (dirDocumentFile != null) {
            repos.download.downloadTrack(
                track = ensureTrackMetadata(track, commit = false),
                repos = repos,
                dirDocumentFile = dirDocumentFile,
            )
        }
    }
}
