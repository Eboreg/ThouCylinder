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
    val localFile: File,
    val width: Int? = null,
    val height: Int? = null,
    val url: String? = null,
) : Parcelable {
    val size: Int
        get() = (width ?: 0) * (height ?: 0)

    private suspend fun getFile(): File? {
        if (!localFile.isFile && url != null) {
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
