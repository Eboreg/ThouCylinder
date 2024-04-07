package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.repositories.Repositories
import javax.inject.Inject


@HiltViewModel
class ImageViewModel @Inject constructor(private val repos: Repositories) : ViewModel() {
    suspend fun getAlbumFullImage(uri: Uri?): ImageBitmap? =
        withContext(Dispatchers.IO) { repos.album.getFullImage(uri) }

    suspend fun getAlbumThumbnail(uri: Uri?): ImageBitmap? =
        withContext(Dispatchers.IO) { uri?.let { repos.album.thumbnailCache.getOrNull(it) } }

    suspend fun getArtistImage(combo: ArtistCombo): ImageBitmap? =
        withContext(Dispatchers.IO) { repos.artist.getArtistImage(combo) }

    suspend fun getPlaylistImage(playlistId: String): ImageBitmap? = withContext(Dispatchers.IO) {
        repos.playlist.listPlaylistAlbums(playlistId).firstNotNullOfOrNull { album ->
            album.albumArt?.thumbnailUri?.let { repos.album.thumbnailCache.getOrNull(it) }
        }
    }

    suspend fun getTrackThumbnail(uri: Uri?): ImageBitmap? = uri?.let { repos.track.thumbnailCache.getOrNull(it) }
}
