package us.huseli.thoucylinder.dataclasses

import android.os.Parcelable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import us.huseli.thoucylinder.toBitmap
import us.huseli.thoucylinder.urlRequest
import java.io.File

@Parcelize
data class Image(
    val width: Int,
    val height: Int,
    val localFile: File,
    val url: String,
) : Parcelable {
    val size: Int
        get() = width * height

    private suspend fun getFile(): File? {
        if (!localFile.isFile) {
            withContext(Dispatchers.IO) {
                val conn = urlRequest(url)
                val body = conn.getInputStream().use { it.readBytes() }
                localFile.outputStream().use { it.write(body) }
            }
        }
        return localFile.takeIf { it.isFile }
    }

    suspend fun getImageBitmap(): ImageBitmap? = getFile()?.toBitmap()?.asImageBitmap()
}
