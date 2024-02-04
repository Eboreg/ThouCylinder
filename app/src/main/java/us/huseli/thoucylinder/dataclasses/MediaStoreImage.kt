package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.isRawFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.dpToPx
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.toBitmap
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import us.huseli.thoucylinder.getSquareBitmap
import us.huseli.thoucylinder.square
import us.huseli.thoucylinder.toBitmap
import java.io.File

@Parcelize
@WorkerThread
data class MediaStoreImage(val uri: Uri, val thumbnailUri: Uri) : Parcelable {
    fun deleteDirectoryFiles(context: Context, dirDocumentFile: DocumentFile) {
        listOf("cover.jpg", "cover-thumbnail.jpg").forEach { filename ->
            dirDocumentFile.findFile(filename)?.deleteWithEmptyParentDirs(context)
        }
    }

    fun deleteInternalFiles() {
        if (uri.isRawFile) uri.toFile().delete()
        if (thumbnailUri.isRawFile) thumbnailUri.toFile().delete()
    }

    suspend fun getFullBitmap(context: Context): Bitmap? {
        return uri.getSquareBitmap(context)
        // return DocumentFileCompat.fromUri(context, uri)?.toBitmap(context)?.square()
    }

    suspend fun getThumbnailBitmap(context: Context): Bitmap? =
        thumbnailUri.getSquareBitmap(context)
        // DocumentFileCompat.fromUri(context, thumbnailUri)?.toBitmap(context)?.square() ?: getFullBitmap(context)

    fun saveToAlbumDirectory(context: Context, dirDocumentFile: DocumentFile) {
        deleteDirectoryFiles(context, dirDocumentFile)
        if (uri.isRawFile) {
            uri.toFile().toBitmap()?.square()?.also { fullBitmap ->
                createDocumentFile(dirDocumentFile, "cover.jpg")
                    ?.openOutputStream(context)
                    ?.use { fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            }
        }
        if (thumbnailUri.isRawFile) {
            thumbnailUri.toFile().toBitmap()?.square()?.also { thumbnailBitmap ->
                createDocumentFile(dirDocumentFile, "cover-thumbnail.jpg")
                    ?.openOutputStream(context)
                    ?.use { thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            }
        }
    }

    companion object {
        fun fromBitmap(bitmap: Bitmap, context: Context, album: Album): MediaStoreImage {
            val (fullImageFilename, thumbnailFilename) = getInternalFilenames(album)
            val fullBitmap = bitmap.square().scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)
            val fullImageFile = getInternalFile(context, fullImageFilename)

            deleteInteralFiles(context, album)
            fullImageFile.outputStream().use { fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }

            val thumbnailFile: File = if (fullBitmap.width > context.dpToPx(IMAGE_MAX_DP_THUMBNAIL)) {
                val thumbnailBitmap = fullBitmap.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context)
                getInternalFile(context, thumbnailFilename)
                    .apply { outputStream().use { thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) } }
            } else fullImageFile

            return MediaStoreImage(
                uri = fullImageFile.toUri(),
                thumbnailUri = thumbnailFile.toUri(),
            )
        }

        fun fromUrls(fullImageUrl: String, thumbnailUrl: String? = fullImageUrl) =
            MediaStoreImage(uri = Uri.parse(fullImageUrl), thumbnailUri = Uri.parse(thumbnailUrl ?: fullImageUrl))

        private fun createDocumentFile(dir: DocumentFile?, filename: String): DocumentFile? =
            dir?.createFile("image/jpeg", filename)

        private fun deleteInteralFiles(context: Context, album: Album) {
            getInternalFilenames(album).toList().forEach { filename ->
                getInternalFile(context, filename).delete()
            }
        }

        private fun getInternalFile(context: Context, filename: String) = File(getInternalImageDir(context), filename)

        private fun getInternalFilenames(album: Album): Pair<String, String> =
            album.albumId.toString().let { Pair("$it.jpg", "$it-thumbnail.jpg") }

        private fun getInternalImageDir(context: Context) = File(context.filesDir, "images").apply { mkdirs() }
    }
}
