package us.huseli.thoucylinder.interfaces

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap

interface IExternalAlbum {
    val id: String
    val title: String
    val artistName: String?

    suspend fun getThumbnailImageBitmap(context: Context): ImageBitmap?
}
