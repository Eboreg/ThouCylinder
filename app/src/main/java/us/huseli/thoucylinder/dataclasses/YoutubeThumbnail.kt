package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import androidx.compose.ui.graphics.ImageBitmap
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

    suspend fun getImageBitmap(): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            Request(url).getImageBitmap()
        } catch (_: FileNotFoundException) {
            null
        }
    }
}
