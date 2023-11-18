package us.huseli.thoucylinder.dataclasses

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.Request
import java.io.FileNotFoundException

@Parcelize
data class SpotifyAlbumArt(
    val url: String,
    val height: Int?,
    val width: Int?,
) : Parcelable {
    val size: Int
        get() = (height ?: 0) * (width ?: 0)

    suspend fun getBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            Request(url).getBitmap()
        } catch (_: FileNotFoundException) {
            null
        }
    }
}
