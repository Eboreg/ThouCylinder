package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.dpToPx
import us.huseli.retaintheme.scaleToMaxSize
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import us.huseli.thoucylinder.toBitmap

@Parcelize
@WorkerThread
data class MediaStoreImage(val uri: Uri, val thumbnailUri: Uri) : Parcelable {
    fun delete(context: Context) {
        DocumentFileCompat.fromUri(context, uri)?.deleteWithEmptyParentDirs(context)
        DocumentFileCompat.fromUri(context, thumbnailUri)?.deleteWithEmptyParentDirs(context)
    }

    fun getFullImageBitmap(context: Context): Bitmap? = DocumentFileCompat.fromUri(context, uri)?.toBitmap(context)

    fun getThumbnailBitmap(context: Context): Bitmap? =
        DocumentFileCompat.fromUri(context, thumbnailUri)?.toBitmap(context) ?: getFullImageBitmap(context)

    companion object {
        fun fromBitmap(
            bitmap: Bitmap,
            context: Context,
            dirDocumentFile: DocumentFile,
        ): MediaStoreImage {
            dirDocumentFile.findFile("cover.jpg")?.delete()
            val imageDocumentFile = dirDocumentFile.createFile("image/jpeg", "cover.jpg")
            checkNotNull(imageDocumentFile)

            val fullBitmap = imageDocumentFile.openOutputStream(context)!!.use { outputStream ->
                bitmap.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)
                    .also { it.compress(Bitmap.CompressFormat.JPEG, 85, outputStream) }
            }
            val thumbnailDocumentFile = if (fullBitmap.width > context.dpToPx(IMAGE_MAX_DP_THUMBNAIL)) {
                dirDocumentFile.findFile("cover-thumbnail.jpg")?.delete()
                dirDocumentFile.createFile("image/jpeg", "cover-thumbnail.jpg")?.also {
                    it.openOutputStream(context)?.use { outputStream ->
                        fullBitmap.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context).also { bitmap ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        }
                    }
                }
            } else null

            return MediaStoreImage(
                uri = imageDocumentFile.uri,
                thumbnailUri = thumbnailDocumentFile?.uri ?: imageDocumentFile.uri,
            )
        }
    }
}
