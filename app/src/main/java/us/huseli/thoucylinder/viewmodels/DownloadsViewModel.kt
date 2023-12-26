package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retaintheme.scaleToMaxSize
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.getBitmapByUrl
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
open class DownloadsViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    val trackDownloadTasks: Flow<List<TrackDownloadTask>> = repos.download.tasks
    val musicDownloadUri: StateFlow<Uri?> = repos.settings.musicDownloadUri

    fun cancelAlbumDownload(albumId: UUID) {
        repos.youtube.albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    fun downloadAlbum(
        pojo: AlbumWithTracksPojo,
        context: Context,
        onFinish: (hasErrors: Boolean) -> Unit = {},
        onTrackError: (Track, Throwable) -> Unit = { _, _ -> },
        onTrackFinish: (Track) -> Unit = {},
    ) = viewModelScope.launch(Dispatchers.IO) {
        getAlbumDownloadDirDocumentFile(pojo.album, context)?.also { dirDocumentFile ->
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
            val imageUrl = pojo.spotifyAlbum?.fullImage?.url ?: pojo.album.youtubePlaylist?.fullImage?.url
            imageUrl?.getBitmapByUrl()?.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)?.let { bitmap -> MediaStoreImage.fromBitmap(bitmap, context, dirDocumentFile) }
            val albumArt = pojo.album.albumArt ?: imageUrl
                ?.getBitmapByUrl()
                ?.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)
                ?.let { bitmap -> MediaStoreImage.fromBitmap(bitmap, context, dirDocumentFile) }

            repos.youtube.addAlbumDownloadTask(AlbumDownloadTask(pojo.album, trackTasks, onFinish))

            withContext(Dispatchers.Main) {
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

    fun downloadTrack(track: Track) = viewModelScope.launch {
        val dirDocumentFile = repos.settings.getMusicDownloadDocumentFile()

        if (dirDocumentFile != null) {
            repos.download.downloadTrack(
                track = ensureTrackMetadata(track, commit = false),
                repos = repos,
                dirDocumentFile = dirDocumentFile,
            )
        }
    }

    suspend fun getAlbumWithTracks(albumId: UUID): AlbumWithTracksPojo? = repos.album.getAlbumWithTracks(albumId)
}
