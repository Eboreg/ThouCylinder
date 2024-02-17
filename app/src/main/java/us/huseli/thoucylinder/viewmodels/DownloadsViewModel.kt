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
        repos.album.getAlbumWithTracks(albumId)?.also { combo ->
            repos.localMedia.createAlbumDirectory(combo.album)?.also { directory ->
                val tracks = combo.tracks
                    .filter { !it.isDownloaded }
                    .map { repos.youtube.ensureTrackMetadata(it) }
                val trackTasks = tracks.map { track ->
                    repos.download.downloadTrack(
                        track = track,
                        repos = repos,
                        directory = directory,
                        albumCombo = combo,
                        onError = { onTrackError(track, it) },
                        onFinish = onTrackFinish,
                    )
                }
                val albumArt = combo.album.albumArt?.saveInternal(combo.album, context)

                albumArt?.saveToDirectory(context, directory)
                repos.youtube.addAlbumDownloadTask(AlbumDownloadTask(combo.album, trackTasks, onFinish))
                repos.album.updateAlbum(
                    combo.album.copy(
                        isLocal = true,
                        isInLibrary = true,
                        albumArt = albumArt,
                    )
                )
            }
        }
    }

    fun downloadTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) {
        repos.settings.getLocalMusicDirectory()?.also { directory ->
            repos.download.downloadTrack(
                track = repos.youtube.ensureTrackMetadata(track),
                repos = repos,
                directory = directory,
            )
        }
    }
}
