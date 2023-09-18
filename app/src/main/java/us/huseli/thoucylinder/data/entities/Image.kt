package us.huseli.thoucylinder.data.entities

import java.io.File
import java.net.URL

data class Image(
    val width: Int,
    val height: Int,
    val url: URL? = null,
    val localPath: File? = null,
) {
    val size: Int
        get() = width * height
}
