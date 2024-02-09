package us.huseli.thoucylinder.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.thoucylinder.Repositories
import us.huseli.thoucylinder.dataclasses.MediaStoreImage
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.combos.AlbumWithTracksCombo
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditAlbumViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel(repos) {
    data class AlbumArt(
        val mediaStoreImage: MediaStoreImage,
        val imageBitmap: ImageBitmap,
        val isCurrent: Boolean = false,
    )

    private val _isLoadingAlbumArt = MutableStateFlow(false)
    private val albumArtMutex = Mutex()

    val allGenres = repos.album.flowGenres()
    val isLoadingAlbumArt = _isLoadingAlbumArt.asStateFlow()

    private suspend fun addAlbumArt(
        url: String,
        context: Context,
        flow: MutableStateFlow<List<AlbumArt>>,
        isCurrent: Boolean = false,
    ) = addAlbumArt(MediaStoreImage.fromUrls(url), context, flow, isCurrent)

    private suspend fun addAlbumArt(
        uri: Uri,
        context: Context,
        flow: MutableStateFlow<List<AlbumArt>>,
        isCurrent: Boolean = false,
    ) = addAlbumArt(MediaStoreImage(uri), context, flow, isCurrent)

    private suspend fun addAlbumArt(
        mediaStoreImage: MediaStoreImage,
        context: Context,
        flow: MutableStateFlow<List<AlbumArt>>,
        isCurrent: Boolean = false,
    ) {
        mediaStoreImage.getFullImageBitmap(context)?.also {
            albumArtMutex.withLock {
                flow.value += AlbumArt(mediaStoreImage, it, isCurrent)
            }
        }
    }

    fun flowAlbumArt2(albumId: UUID, context: Context) =
        MutableStateFlow<List<AlbumArt>>(emptyList()).also { flow ->
            _isLoadingAlbumArt.value = true

            viewModelScope.launch(Dispatchers.IO) {
                repos.album.getAlbumWithTracks(albumId)?.also { combo ->
                    val jobs = listOf(
                        launch {
                            combo.album.albumArt?.also { addAlbumArt(it, context, flow, true) }
                        },
                        launch {
                            combo.spotifyAlbum?.fullImage?.url?.also { addAlbumArt(it, context, flow) }
                        },
                        launch {
                            combo.album.youtubePlaylist?.fullImage?.url?.also { addAlbumArt(it, context, flow) }
                        },
                        launch {
                            repos.localMedia.collectNewLocalAlbumArtUris(combo).forEach {
                                addAlbumArt(it, context, flow)
                            }
                        },
                        launch {
                            repos.discogs.searchMasters(
                                query = combo.album.title,
                                artist = combo.album.artist,
                            )?.data?.results?.forEach { item ->
                                repos.discogs.getMaster(item.id)?.data?.images
                                    ?.filter { image -> image.type == "primary" }
                                    ?.forEach { addAlbumArt(it.uri, context, flow) }
                            }
                        },
                        launch {
                            combo.album.musicBrainzReleaseId?.also { releaseId ->
                                repos.musicBrainz.getSiblingReleaseIds(releaseId).forEach { siblingId ->
                                    repos.musicBrainz.getAllCoverArtArchiveImages(siblingId).forEach {
                                        addAlbumArt(it.image, context, flow)
                                    }
                                }
                            }
                        },
                    )

                    jobs.joinAll()
                    _isLoadingAlbumArt.value = false
                }
            }
        }.asStateFlow()

    fun flowAlbumWithTracks(albumId: UUID) = repos.album.flowAlbumWithTracks(albumId)

    fun saveAlbumCombo(combo: AbstractAlbumCombo) = viewModelScope.launch(Dispatchers.IO) {
        repos.album.updateAlbum(combo.album)
        repos.album.saveAlbumGenres(combo.album.albumId, combo.genres)
    }

    fun saveAlbumArtFromUri(albumId: UUID, uri: Uri, context: Context, onSuccess: () -> Unit, onFail: () -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.album.getAlbum(albumId)?.also { album ->
                MediaStoreImage(uri).saveInternal(album, context)?.also { mediaStoreImage ->
                    repos.localMedia.deleteAlbumDirectoryAlbumArt(album)
                    repos.localMedia.createAlbumDirectory(album)?.also {
                        mediaStoreImage.saveToDirectory(context, it)
                    }
                    repos.album.updateAlbumArt(albumId, mediaStoreImage)
                    onSuccess()
                } ?: run(onFail)
            } ?: run(onFail)
        }

    fun saveAlbumArt(albumId: UUID, albumArt: AlbumArt?, context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            repos.album.getAlbum(albumId)?.also { album ->
                val mediaStoreImage = albumArt?.mediaStoreImage?.saveInternal(album, context)

                repos.localMedia.deleteAlbumDirectoryAlbumArt(album)
                if (album.isLocal && mediaStoreImage != null) {
                    repos.localMedia.createAlbumDirectory(album)?.also {
                        mediaStoreImage.saveToDirectory(context, it)
                    }
                }
                repos.album.updateAlbumArt(albumId, mediaStoreImage)
            }
        }

    fun tagAlbumTrack(combo: AlbumWithTracksCombo, track: Track) = viewModelScope.launch(Dispatchers.IO) {
        repos.localMedia.tagTrack(ensureTrackMetadata(track), combo)
    }

    fun tagAlbumTracks(combo: AlbumWithTracksCombo) = viewModelScope.launch(Dispatchers.IO) {
        repos.localMedia.tagAlbumTracks(ensureAlbumTracksMetadata(combo))
    }
}
