package us.huseli.thoucylinder.dataclasses

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import java.io.File
import java.io.FileNotFoundException

data class MediaStoreEntry(
    val id: Long,
    val file: File,
    val relativePath: String,
    val filename: String,
    val contentUri: Uri,
) {
    private val parentDirNames: List<String>
        get() = relativePath.trim('/').split('/').drop(1).reversed()

    fun deleteWithEmptyParentDirs(context: Context) {
        if (file.isFile && file.canWrite()) {
            var parentDir = file.parentFile

            file.delete()
            for (dirName in parentDirNames) {
                if (
                    parentDir?.isDirectory == true &&
                    parentDir.name == dirName &&
                    parentDir.list()?.isEmpty() == true &&
                    parentDir.canWrite()
                ) {
                    parentDir.delete()
                    parentDir = parentDir.parentFile
                }
            }
        }
        try {
            context.contentResolver.delete(contentUri, null, null)
        } catch (_: Exception) {
        }
    }
}


fun Context.deleteMediaStoreUriAndFile(contentUri: Uri) =
    getMediaStoreEntry(contentUri)?.deleteWithEmptyParentDirs(this)


fun Context.deleteMediaStoreUriAndFile(collection: Uri, relativePath: String, filename: String) = getMediaStoreEntry(
    collection = collection,
    selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?",
    selectionArgs = arrayOf(filename, relativePath),
)?.deleteWithEmptyParentDirs(this)


fun Context.getMediaStoreEntries(
    collection: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    limit: Int? = null,
): List<MediaStoreEntry> {
    val projection = arrayOf(MediaColumns._ID, MediaColumns.RELATIVE_PATH, MediaColumns.DATA, MediaColumns.DISPLAY_NAME)
    val entries = mutableListOf<MediaStoreEntry>()

    contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
        val dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val relativePathColumn = cursor.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)
        val idColumn = cursor.getColumnIndexOrThrow(MediaColumns._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            if (limit != null && entries.size == limit) break
            val id = cursor.getLong(idColumn)

            entries.add(
                MediaStoreEntry(
                    id = id,
                    file = File(cursor.getString(dataColumn)),
                    relativePath = cursor.getString(relativePathColumn),
                    contentUri = ContentUris.withAppendedId(collection, id),
                    filename = cursor.getString(displayNameColumn),
                )
            )
        }
    }
    return entries
}


fun Context.getMediaStoreEntry(
    collection: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
): MediaStoreEntry? = getMediaStoreEntries(collection, selection, selectionArgs, 1).firstOrNull()


fun Context.getMediaStoreEntry(contentUri: Uri): MediaStoreEntry? {
    val projection = arrayOf(MediaColumns._ID, MediaColumns.RELATIVE_PATH, MediaColumns.DATA, MediaColumns.DISPLAY_NAME)
    var entry: MediaStoreEntry? = null

    contentResolver.query(contentUri, projection, null, null)?.use { cursor ->
        if (cursor.moveToNext()) {
            entry = MediaStoreEntry(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaColumns._ID)),
                file = File(cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.DATA))),
                relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)),
                contentUri = contentUri,
                filename = cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)),
            )
        }
    }
    return entry
}


fun Context.getMediaStoreFile(contentUri: Uri): File =
    getMediaStoreFileNullable(contentUri) ?: throw FileNotFoundException(contentUri.path)


fun Context.getMediaStoreFileNullable(contentUri: Uri): File? = getMediaStoreEntry(contentUri)?.file
