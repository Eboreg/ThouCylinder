package us.huseli.thoucylinder.dataclasses

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.isRawFile
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.square
import us.huseli.thoucylinder.Constants.IMAGE_FULL_MAX_WIDTH_DP
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_DP
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.deleteWithEmptyParentDirs
import us.huseli.thoucylinder.getBitmap
import us.huseli.thoucylinder.getBitmapByUrl
import us.huseli.thoucylinder.isRemote
import java.io.File

@Parcelize
@WorkerThread
@Immutable
data class MediaStoreImage(val fullUriString: String, val thumbnailUriString: String, val hash: Int) : Parcelable {
    constructor(fullUriString: String, thumbnailUriString: String) :
        this(fullUriString, thumbnailUriString, fullUriString.hashCode())

    constructor(fullUriString: String) : this(fullUriString, fullUriString, fullUriString.hashCode())

    val fullUri: Uri
        get() = fullUriString.toUri()

    val isLocal: Boolean
        get() = !fullUri.isRemote

    val thumbnailUri: Uri
        get() = thumbnailUriString.toUri()

    fun deleteDirectoryFiles(context: Context, directory: DocumentFile) {
        deleteFullImageFile(context, directory)
        deleteThumbnailImageFile(context, directory)
    }

    fun deleteInternalFiles() {
        if (fullUri.isRawFile) fullUri.toFile().delete()
        if (thumbnailUri.isRawFile) thumbnailUri.toFile().delete()
    }

    suspend fun saveInternal(album: Album, context: Context): MediaStoreImage? {
        if (fullUri.isRawFile) return this

        return getFullBitmap(context)?.let { fullBitmap ->
            val (fullImageFilename, thumbnailFilename) = getInternalFilenames(album)
            val fullImageFile = getInternalFile(context, fullImageFilename)
            val thumbnailBitmap =
                getThumbnailBitmap(context) ?: fullBitmap.scaleToMaxSize(IMAGE_THUMBNAIL_MAX_WIDTH_DP.dp, context)
            val thumbnailFile = getInternalFile(context, thumbnailFilename)

            deleteInteralFiles(context, album)
            fullImageFile.outputStream().use { fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            thumbnailFile.outputStream().use { thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }

            MediaStoreImage(
                fullUriString = fullImageFile.toUri().toString(),
                thumbnailUriString = thumbnailFile.toUri().toString(),
                hash = fullBitmap.hashCode(),
            )
        }
    }

    suspend fun saveToDirectory(context: Context, directory: DocumentFile) {
        getFullBitmap(context)?.also { fullBitmap ->
            deleteFullImageFile(context, directory)
            createDocumentFile(directory, "cover.jpg")
                ?.openOutputStream(context)
                ?.use { fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
        getThumbnailBitmap(context)?.also { thumbnailBitmap ->
            deleteThumbnailImageFile(context, directory)
            createDocumentFile(directory, "cover-thumbnail.jpg")
                ?.openOutputStream(context)
                ?.use { thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
    }

    private suspend fun getFullBitmap(context: Context): Bitmap? =
        fullUri.getBitmap(context)?.square()?.scaleToMaxSize(IMAGE_FULL_MAX_WIDTH_DP.dp, context)

    private suspend fun getThumbnailBitmap(context: Context): Bitmap? =
        thumbnailUri.getBitmap(context)?.square()?.scaleToMaxSize(IMAGE_THUMBNAIL_MAX_WIDTH_DP.dp, context)

    companion object {
        private fun createDocumentFile(dir: DocumentFile, filename: String): DocumentFile? =
            dir.createFile("image/jpeg", filename)

        private fun deleteFullImageFile(context: Context, directory: DocumentFile) {
            directory.findFile("cover.jpg")?.deleteWithEmptyParentDirs(context)
        }

        private fun deleteInteralFiles(context: Context, album: Album) {
            getInternalFilenames(album).toList().forEach { filename ->
                getInternalFile(context, filename).delete()
            }
        }

        private fun deleteThumbnailImageFile(context: Context, directory: DocumentFile) {
            directory.findFile("cover-thumbnail.jpg")?.deleteWithEmptyParentDirs(context)
        }

        private fun getInternalFile(context: Context, filename: String) = File(getInternalImageDir(context), filename)

        private fun getInternalFilenames(album: Album): Pair<String, String> =
            Pair("${album.albumId}.jpg", "${album.albumId}-thumbnail.jpg")

        private fun getInternalImageDir(context: Context) = File(context.filesDir, "images").apply { mkdirs() }
    }
}

suspend fun String.toMediaStoreImage(thumbnailUrl: String? = null): MediaStoreImage? =
    getBitmapByUrl()?.let { bitmap ->
        MediaStoreImage(
            fullUriString = this,
            thumbnailUriString = thumbnailUrl ?: this,
            hash = bitmap.hashCode(),
        )
    }

suspend fun Uri.toMediaStoreImage(context: Context? = null): MediaStoreImage? = getBitmap(context)?.let { bitmap ->
    MediaStoreImage(
        fullUriString = toString(),
        thumbnailUriString = toString(),
        hash = bitmap.hashCode(),
    )
}
