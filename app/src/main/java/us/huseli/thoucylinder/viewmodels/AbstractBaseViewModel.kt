package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import java.util.UUID

abstract class AbstractBaseViewModel(private val repos: Repositories) : ViewModel() {
    private val _albumThumbnailCache = mutableMapOf<UUID, ImageBitmap>()
    private val _trackThumbnailCache = mutableMapOf<UUID, ImageBitmap>()

    suspend fun ensureTrackMetadata(track: Track, commit: Boolean, forceReload: Boolean = false): Track {
        var changed = false
        val youtubeMetadata = track.youtubeVideo?.metadata
            ?: repos.youtube.getBestMetadata(track, forceReload = forceReload)?.also { changed = true }
        val metadata = track.metadata ?: youtubeMetadata?.toTrackMetadata()?.also { changed = true }

        if (!changed) return track

        return track.copy(
            metadata = metadata,
            youtubeVideo = track.youtubeVideo?.copy(metadata = youtubeMetadata),
        ).also { if (commit) withContext(Dispatchers.Main) { repos.track.updateTrack(it) } }
    }

    @WorkerThread
    fun getAlbumDownloadDirDocumentFile(album: Album, context: Context): DocumentFile? =
        repos.settings.getMusicDownloadDocumentFile()?.let { album.getDownloadDirDocumentFile(it, context) }

    suspend fun getTrackThumbnail(
        track: Track,
        albumPojo: AbstractAlbumPojo? = null,
        album: Album? = albumPojo?.album,
        context: Context,
    ): ImageBitmap? {
        _trackThumbnailCache[track.trackId]?.also { return it }
        track.getThumbnail(context)?.also {
            _trackThumbnailCache += track.trackId to it
            return it
        }

        if (album != null) {
            _albumThumbnailCache[album.albumId]?.also { return it }
            album.getThumbnail(context)?.also {
                _albumThumbnailCache += album.albumId to it
                return it
            }

            val spotifyAlbum =
                if (albumPojo != null) albumPojo.spotifyAlbum
                else repos.spotify.getSpotifyAlbum(album.albumId)

            spotifyAlbum?.getThumbnail(context)?.also {
                _albumThumbnailCache += album.albumId to it
                return it
            }
        }

        return null
    }

    fun importNewLocalAlbums(context: Context, existingTracks: List<Track>? = null) = viewModelScope.launch {
        repos.localMedia.setIsImporting(true)

        val existingTrackUris = existingTracks?.mapNotNull { it.localUri } ?: repos.track.listTrackLocalUris()
        val newAlbumPojos = repos.localMedia.listNewLocalAlbums(existingTrackUris).map { pojo ->
            // The album could potentially be spread across multiple
            // directories, but just use the first one in that case.
            val albumArt = withContext(Dispatchers.IO) {
                pojo.listTrackParentDirectories(context).firstOrNull()?.let { dirDocumentFile ->
                    repos.localMedia.collectAlbumImages(pojo).maxByOrNull { it.width * it.height }?.let {
                        MediaStoreImage.fromBitmap(
                            bitmap = it,
                            context = context,
                            dirDocumentFile = dirDocumentFile,
                        )
                    }
                }
            }
            pojo.copy(album = pojo.album.copy(albumArt = albumArt))
        }

        newAlbumPojos.forEach { pojo -> repos.album.saveAlbumPojo(pojo) }
        newAlbumPojos.flatMap { it.tracks }.also { repos.track.insertTracks(it) }
        repos.localMedia.setIsImporting(false)
    }

    fun saveAlbumWithTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch {
        repos.album.saveAlbumPojo(pojo)
        repos.track.deleteTracksByAlbumId(pojo.album.albumId)
        repos.track.insertTracks(pojo.tracks.map { it.copy(albumId = pojo.album.albumId, isInLibrary = true) })
    }
}
