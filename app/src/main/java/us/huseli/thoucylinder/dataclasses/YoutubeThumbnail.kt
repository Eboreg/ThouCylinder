package us.huseli.thoucylinder.dataclasses

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.Request
import java.io.FileNotFoundException

@Parcelize
data class YoutubeThumbnail(
    val url: String,
    val width: Int,
    val height: Int,
) : Parcelable {
    val size: Int
        get() = width * height

    suspend fun getBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            Request(url).getBitmap()
        } catch (_: FileNotFoundException) {
            null
        }
    }
}
