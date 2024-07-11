package us.huseli.thoucylinder.repositories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.track.ITrackCombo
import us.huseli.thoucylinder.dataclasses.track.listCoverImages
import us.huseli.thoucylinder.getFullBitmap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(@ApplicationContext private val context: Context) {
    fun collectNewLocalAlbumArtUris(combo: IAlbumWithTracksCombo<IAlbum>): List<Uri> =
        combo.trackCombos.map { it.track }.listCoverImages(context)
            .map { it.uri }
            .filter { it != combo.album.albumArt?.fullUri }

    suspend fun getFullBitmap(uri: Uri?): Bitmap? = uri?.getFullBitmap(context, saveToCache = false)

    suspend fun getTrackComboFullImageBitmap(trackCombo: ITrackCombo): ImageBitmap? =
        getTrackComboFullBitmap(trackCombo)?.asImageBitmap()

    private suspend fun getTrackComboFullBitmap(trackCombo: ITrackCombo): Bitmap? =
        trackCombo.album?.albumArt?.fullUri?.getFullBitmap(context, saveToCache = trackCombo.track.isInLibrary)
            ?: trackCombo.track.image?.fullUri?.getFullBitmap(context, saveToCache = trackCombo.track.isInLibrary)
}
