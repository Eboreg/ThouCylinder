package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.launchOnIOThread
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
open class DownloadsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val trackDownloadTasks: Flow<ImmutableList<TrackDownloadTask>> = repos.download.tasks
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri

    fun cancelAlbumDownload(albumId: String) {
        repos.youtube.albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    fun downloadAlbum(
        albumId: String,
        context: Context,
        onFinish: (hasErrors: Boolean) -> Unit = {},
        onTrackError: (TrackCombo, Throwable) -> Unit = { _, _ -> },
        onTrackFinish: (Track) -> Unit = {},
    ) = launchOnIOThread {
        repos.album.getAlbumWithTracks(albumId)?.also { albumCombo ->
            repos.settings.createAlbumDirectory(albumCombo)?.also { directory ->
                val trackCombos = albumCombo.trackCombos
                    .filter { !it.track.isDownloaded }
                    .map { it.copy(track = repos.youtube.ensureTrackMetadata(track = it.track)) }
                val trackTasks = trackCombos.map { trackCombo ->
                    repos.download.downloadTrack(
                        track = trackCombo.track,
                        repos = repos,
                        directory = directory,
                        onError = { onTrackError(trackCombo, it) },
                        onFinish = onTrackFinish,
                        trackArtists = trackCombo.artists,
                        album = albumCombo.album,
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

    fun downloadTrack(state: Track.ViewState) = launchOnIOThread {
        repos.settings.getLocalMusicDirectory()?.also { directory ->
            repos.download.downloadTrack(
                state = state,
                repos = repos,
                directory = directory,
            )
        }
    }
}
