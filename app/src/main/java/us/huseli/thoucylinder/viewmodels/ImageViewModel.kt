package us.huseli.thoucylinder.viewmodels

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.thoucylinder.dataclasses.views.ArtistCombo
import us.huseli.thoucylinder.managers.Managers
import javax.inject.Inject


@HiltViewModel
class ImageViewModel @Inject constructor(private val managers: Managers) : ViewModel() {
    suspend fun getFullImageBitmap(uri: Uri?): ImageBitmap? = managers.image.getFullImageBitmap(uri)

    suspend fun getThumbnailImageBitmap(uri: Uri?): ImageBitmap? = managers.image.getThumbnailImageBitmap(uri)

    suspend fun getArtistThumbnailImageBitmap(combo: ArtistCombo): ImageBitmap? =
        managers.image.getArtistThumbnailImageBitmap(combo)

    suspend fun getPlaylistThumbnailImageBitmap(playlistId: String): ImageBitmap? =
        managers.image.getPlaylistThumbnailImageBitmap(playlistId)
}
