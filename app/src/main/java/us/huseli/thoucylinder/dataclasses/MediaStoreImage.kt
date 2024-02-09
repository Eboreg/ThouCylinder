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
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.dpToPx
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.square
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.asFullImageBitmap
import us.huseli.thoucylinder.asThumbnailImageBitmap
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import us.huseli.thoucylinder.getSquareBitmap
import java.io.File

@Parcelize
@WorkerThread
data class MediaStoreImage(val uri: Uri, val thumbnailUri: Uri) : Parcelable {
    constructor(uri: Uri) : this(uri, uri)

    fun deleteDirectoryFiles(context: Context, directory: DocumentFile) {
        listOf("cover.jpg", "cover-thumbnail.jpg").forEach { filename ->
            directory.findFile(filename)?.deleteWithEmptyParentDirs(context)
        }
    }

    fun deleteInternalFiles() {
        if (uri.isRawFile) uri.toFile().delete()
        if (thumbnailUri.isRawFile) thumbnailUri.toFile().delete()
    }

    suspend fun getFullBitmap(context: Context): Bitmap? = uri.getSquareBitmap(context)

    suspend fun getFullImageBitmap(context: Context) = getFullBitmap(context)?.asFullImageBitmap(context)

    suspend fun getThumbnailImageBitmap(context: Context) = getThumbnailBitmap(context)?.asThumbnailImageBitmap(context)

    suspend fun saveInternal(album: Album, context: Context): MediaStoreImage? {
        if (uri.isRawFile) return this
        return getFullBitmap(context)?.let { fromBitmap(it, context, album) }
    }

    suspend fun saveToDirectory(context: Context, directory: DocumentFile) {
        deleteDirectoryFiles(context, directory)
        getFullBitmap(context)?.also { fullBitmap ->
            createDocumentFile(directory, "cover.jpg")
                ?.openOutputStream(context)
                ?.use { fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
        getThumbnailBitmap(context)?.also { thumbnailBitmap ->
            createDocumentFile(directory, "cover-thumbnail.jpg")
                ?.openOutputStream(context)
                ?.use { thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
    }

    private suspend fun getThumbnailBitmap(context: Context): Bitmap? = thumbnailUri.getSquareBitmap(context)

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

        private fun createDocumentFile(dir: DocumentFile, filename: String): DocumentFile? =
            dir.createFile("image/jpeg", filename)

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
