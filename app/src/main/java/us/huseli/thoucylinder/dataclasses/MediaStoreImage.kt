package us.huseli.thoucylinder.dataclasses

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Size
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize
import us.huseli.retaintheme.sanitizeFilename
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import us.huseli.thoucylinder.Constants.IMAGE_SUBDIR_ALBUM
import us.huseli.thoucylinder.Constants.IMAGE_SUBDIR_TRACK
import us.huseli.thoucylinder.Request
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.getReadWriteImageCollection
import us.huseli.retaintheme.scaleToMaxSize
import us.huseli.retaintheme.substringMax
import us.huseli.retaintheme.toBitmap
import java.io.File
import java.util.UUID


@Parcelize
data class MediaStoreImage(
    val uri: Uri,
    val size: Size,
    val thumbnailUri: Uri? = null,
) : Parcelable {
    fun delete(context: Context) {
        /** Deletes both mediastore entry and physical file. */
        context.deleteMediaStoreUriAndFile(uri)
        thumbnailUri?.also { context.deleteMediaStoreUriAndFile(it) }
    }

    fun ensureThumbnail(context: Context): MediaStoreImage {
        if (thumbnailUri == null) {
            context.getMediaStoreEntry(uri)?.also { entry ->
                val thumbnailUri = getThumbnailBitmap(context)?.let { bitmap ->
                    thumbnailFromBitmap(bitmap, entry.relativePath, entry.filename, context)
                }
                return copy(thumbnailUri = thumbnailUri)
            }
        }
        return this
    }

    fun getBitmap(context: Context): Bitmap? = context.getMediaStoreFileNullable(uri)?.toBitmap()

    fun getFile(context: Context): File? = context.getMediaStoreFileNullable(uri)

    fun getThumbnailBitmap(context: Context): Bitmap? =
        thumbnailUri?.let { context.getMediaStoreFileNullable(it)?.toBitmap() }
            ?: getBitmap(context)?.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context)

    companion object {
        private val albumRelativePath = "${Environment.DIRECTORY_PICTURES}/$IMAGE_SUBDIR_ALBUM"
        private val trackRelativePath = "${Environment.DIRECTORY_PICTURES}/$IMAGE_SUBDIR_TRACK"

        fun fromBitmap(bitmap: Bitmap, album: Album, context: Context): MediaStoreImage =
            fromBitmap(
                bitmap = bitmap,
                relativePath = albumRelativePath,
                filename = getFilename(album.title, album.artist, album.albumId),
                context = context,
            )

        suspend fun fromUrl(url: String, playlist: YoutubePlaylist, context: Context) = fromUrl(
            url = url,
            relativePath = albumRelativePath,
            filename = getFilename(playlist.title, playlist.artist),
            context = context,
        )

        suspend fun fromUrl(url: String, video: YoutubeVideo, context: Context) = fromUrl(
            url = url,
            relativePath = trackRelativePath,
            filename = getFilename(video.title),
            context = context,
        )

        @Suppress("SameParameterValue")
        private fun fromBitmap(
            bitmap: Bitmap,
            relativePath: String,
            filename: String,
            context: Context,
        ): MediaStoreImage {
            val uri = getContentUri(relativePath, filename, context)
            val fullSize = context.contentResolver.openOutputStream(uri, "w")!!.use { outputStream ->
                val fullBitmap = bitmap.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)
                fullBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                Size(fullBitmap.width, fullBitmap.height)
            }
            val thumbnailUri = thumbnailFromBitmap(bitmap, relativePath, filename, context)

            return MediaStoreImage(uri = uri, size = fullSize, thumbnailUri = thumbnailUri)
        }

        private suspend fun fromUrl(
            url: String,
            relativePath: String,
            filename: String,
            context: Context,
        ): MediaStoreImage {
            val uri = getContentUri(relativePath, filename, context)
            val fullBitmap = context.contentResolver.openOutputStream(uri, "w")!!.use { outputStream ->
                val bitmap = Request(url).getBitmap()?.scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context)
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                bitmap
            }
            checkNotNull(fullBitmap)
            val fullSize = Size(fullBitmap.width, fullBitmap.height)
            val thumbnailUri = thumbnailFromBitmap(fullBitmap, relativePath, filename, context)

            return MediaStoreImage(uri = uri, size = fullSize, thumbnailUri = thumbnailUri)
        }

        private fun getContentUri(relativePath: String, filename: String, context: Context): Uri {
            val uri = context.contentResolver.insert(
                getReadWriteImageCollection(),
                ContentValues().apply {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                },
            )
            return checkNotNull(uri)
        }

        private fun getFilename(title: String, artist: String? = null, id: UUID? = null): String {
            val basename = ((artist?.let { "$it - " } ?: "") + title).substringMax(0, 200) + "-$id"
            return "${basename.sanitizeFilename()}.jpg"
        }

        private fun thumbnailFromBitmap(
            bitmap: Bitmap,
            relativePath: String,
            fullFilename: String,
            context: Context,
        ): Uri {
            val thumbnailFilename = File(fullFilename).let { "${it.nameWithoutExtension}-thumbnail.${it.extension}" }
            val thumbnailUri = getContentUri(relativePath, thumbnailFilename, context)

            context.contentResolver.openOutputStream(thumbnailUri, "w")!!.use { outputStream ->
                val thumbnailBitmap = bitmap.scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context)
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }
            return thumbnailUri
        }
    }
}
