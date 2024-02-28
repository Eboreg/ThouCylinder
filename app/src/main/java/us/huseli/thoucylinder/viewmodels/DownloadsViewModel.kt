package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.combos.TrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.launchOnIOThread
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
        onTrackError: (TrackCombo, Throwable) -> Unit = { _, _ -> },
        onTrackFinish: (Track) -> Unit = {},
    ) = launchOnIOThread {
        repos.album.getAlbumWithTracks(albumId)?.also { albumCombo ->
            repos.settings.createAlbumDirectory(albumCombo)?.also { directory ->
                val trackCombos = albumCombo.trackCombos
                    .filter { !it.track.isDownloaded }
                    .map { it.copy(track = repos.youtube.ensureTrackMetadata(it.track)) }
                val trackTasks = trackCombos.map { trackCombo ->
                    repos.download.downloadTrack(
                        trackCombo = trackCombo,
                        repos = repos,
                        directory = directory,
                        albumCombo = albumCombo,
                        onError = { onTrackError(trackCombo, it) },
                        onFinish = onTrackFinish,
                    )
                }
                val albumArt = albumCombo.album.albumArt?.saveInternal(albumCombo.album, context)

                albumArt?.saveToDirectory(context, directory)
                repos.youtube.addAlbumDownloadTask(AlbumDownloadTask(albumCombo.album, trackTasks, onFinish))
                repos.album.updateAlbum(
                    albumCombo.album.copy(
                        isLocal = true,
                        isInLibrary = true,
                        albumArt = albumArt,
                    )
                )
            }
        }
    }

    fun downloadTrack(trackCombo: AbstractTrackCombo) = launchOnIOThread {
        repos.settings.getLocalMusicDirectory()?.also { directory ->
            repos.download.downloadTrack(
                trackCombo = trackCombo,
                repos = repos,
                directory = directory,
            )
        }
    }
}
