package us.huseli.thoucylinder.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.asFullImageBitmap
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.getSquareBitmapByUrl
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditAlbumViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    private val _isLoadingAlbumArt = MutableStateFlow(false)

    val allGenres = repos.album.flowGenres()
    val isLoadingAlbumArt = _isLoadingAlbumArt.asStateFlow()

    fun flowAlbumArt(albumId: UUID, context: Context) =
        MutableStateFlow<List<ImageBitmap>>(emptyList()).also { flow ->
            _isLoadingAlbumArt.value = true
            viewModelScope.launch(Dispatchers.IO) {
                repos.album.getAlbumWithTracks(albumId)?.also { pojo ->
                    pojo.album.albumArt
                        ?.getFullBitmap(context)
                        ?.asFullImageBitmap(context)
                        ?.also { flow.value += it }
                    pojo.spotifyAlbum?.fullImage?.url
                        ?.getSquareBitmapByUrl()
                        ?.asFullImageBitmap(context)
                        ?.also { flow.value += it }
                    pojo.album.youtubePlaylist?.fullImage?.url
                        ?.getSquareBitmapByUrl()
                        ?.asFullImageBitmap(context)
                        ?.also { flow.value += it }
                    pojo.lastFmAlbum?.fullImageUrl
                        ?.getSquareBitmapByUrl()
                        ?.asFullImageBitmap(context)
                        ?.also { flow.value += it }
                    flow.value += repos.localMedia.collectAlbumArt(pojo).map { it.asFullImageBitmap(context) }
                    repos.discogs.searchMasters(
                        query = pojo.album.title,
                        artist = pojo.album.artist,
                    )?.data?.results?.forEach { item ->
                        repos.discogs.getMaster(item.id)?.data?.images
                            ?.filter { image -> image.type == "primary" }
                            ?.forEach { image ->
                                image.uri.getSquareBitmapByUrl()?.asFullImageBitmap(context)?.also { bitmap ->
                                    flow.value += bitmap
                                }
                            }
                    }
                    _isLoadingAlbumArt.value = false
                }
            }
        }.asStateFlow()

    fun flowAlbumWithTracks(albumId: UUID) = repos.album.flowAlbumWithTracks(albumId)

    fun saveAlbumPojo(pojo: AbstractAlbumPojo) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.saveAlbum(pojo.album)
        repos.album.saveAlbumGenres(pojo.album.albumId, pojo.genres)
    }

    fun saveAlbumArt(albumId: UUID, imageBitmap: ImageBitmap?, context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.album.getAlbum(albumId)?.also { album ->
                repos.localMedia.deleteAlbumDirectoryAlbumArt(album)
                album.albumArt?.deleteInternalFiles()

                val albumArt = imageBitmap?.let { MediaStoreImage.fromBitmap(it.asAndroidBitmap(), context, album) }
                if (album.isLocal) {
                    repos.localMedia.createAlbumDocumentFile(album)?.also {
                        albumArt?.saveToAlbumDirectory(context, it)
                    }
                }
                repos.album.saveAlbum(album.copy(albumArt = albumArt))
            }
        }

    fun saveTrack(track: Track) = viewModelScope.launch(Dispatchers.IO) { repos.track.updateTrack(track) }

    fun tagAlbumTrack(pojo: AlbumWithTracksPojo, track: Track) = viewModelScope.launch(Dispatchers.IO) {
        repos.localMedia.tagTrack(ensureTrackMetadata(track), pojo)
    }

    fun tagAlbumTracks(pojo: AlbumWithTracksPojo) = viewModelScope.launch(Dispatchers.IO) {
        repos.localMedia.tagAlbumTracks(ensureAlbumTracksMetadata(pojo))
    }
}
