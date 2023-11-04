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
    val uri: Uri,
) {
    val parentDirNames: List<String>
        get() = relativePath.trim('/').split('/').drop(1)
}


fun Context.getMediaStoreEntries(
    collection: Uri,
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

    contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
        val dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
        val relativePathColumn = cursor.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)
        val idColumn = cursor.getColumnIndexOrThrow(MediaColumns._ID)

        while (cursor.moveToNext()) {
            if (limit != null && entries.size == limit) break
            val id = cursor.getLong(idColumn)

            entries.add(
                MediaStoreEntry(
                    id = id,
                    file = File(cursor.getString(dataColumn)),
                    relativePath = cursor.getString(relativePathColumn),
                    uri = ContentUris.withAppendedId(collection, id),
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
