package us.huseli.thoucylinder.dataclasses

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import java.io.File

data class MediaStoreEntry(
    val id: Long,
    val file: File,
    val relativePath: String,
) {
    val parentDirNames: List<String>
        get() = relativePath.trim('/').split('/').drop(1)

    /** Call with e.g. MediaStore.Audio.Media.EXTERNAL_CONTENT_URI */
    fun getContentUri(baseUri: Uri) = ContentUris.withAppendedId(baseUri, id)
}


fun Context.getMediaStoreEntries(
    queryUri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    limit: Int? = null,
): List<MediaStoreEntry> {
    val projection = arrayOf(
        MediaColumns._ID,
        MediaColumns.RELATIVE_PATH,
        MediaColumns.DATA,
    )
    val entries = mutableListOf<MediaStoreEntry>()

    contentResolver.query(queryUri, projection, selection, selectionArgs, null)?.use { cursor ->
        val dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val relativePathColumn = cursor.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)
        val idColumn = cursor.getColumnIndexOrThrow(MediaColumns._ID)

        while (cursor.moveToNext()) {
            if (limit != null && entries.size == limit) break
            entries.add(
                MediaStoreEntry(
                    id = cursor.getLong(idColumn),
                    file = File(cursor.getString(dataColumn)),
                    relativePath = cursor.getString(relativePathColumn),
                )
            )
        }
    }

    return entries
}


fun Context.getMediaStoreEntry(
    queryUri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
): MediaStoreEntry? = getMediaStoreEntries(queryUri, selection, selectionArgs, 1).firstOrNull()
