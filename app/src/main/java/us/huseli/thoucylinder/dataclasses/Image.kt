package us.huseli.thoucylinder.dataclasses

import us.huseli.thoucylinder.urlRequest
import java.io.File

data class Image(
    val width: Int,
    val height: Int,
    val localFile: File,
    val url: String? = null,
) {
    val size: Int
        get() = width * height

    suspend fun getFile(): File? {
        if (!localFile.isFile) {
            url?.let { url ->
                val conn = urlRequest(url)
                val body = conn.getInputStream().use { it.readBytes() }
                localFile.outputStream().use { it.write(body) }
            }
        }
        return localFile.takeIf { it.isFile }
    }
}
