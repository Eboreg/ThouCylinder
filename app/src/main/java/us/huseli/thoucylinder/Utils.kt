package us.huseli.thoucylinder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.TypedValue
import android.webkit.MimeTypeMap
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.scale
import org.json.JSONObject
import us.huseli.thoucylinder.dataclasses.getMediaStoreEntry
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO


fun List<Duration>.sum(): Duration = this.plus(ZERO).reduce { d1, d2 -> d1 + d2 }

fun File.toBitmap(): Bitmap? = takeIf { it.isFile }?.inputStream().use { BitmapFactory.decodeStream(it) }

fun String.escapeQuotes() = replace("\"", "\\\"")


fun <T> Map<*, *>.yquery(keys: String, failSilently: Boolean = true): T? {
    val splitKeys = keys.split(".", limit = 2)
    val key = splitKeys[0]
    val newKeys = splitKeys.getOrNull(1) ?: ""
    val value = this[key]

    if (newKeys.isEmpty()) {
        @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
        return try {
            val castValue = value as? T
            if (castValue == null && !failSilently)
                throw Exception("$value is not of correct type")
            castValue
        } catch (e: ClassCastException) {
            if (!failSilently)
                throw Exception("$value is not of correct type")
            null
        }
    }
    if (value is Map<*, *>) {
        return value.yquery<T>(keys = newKeys, failSilently = failSilently)
    }
    if (!failSilently) {
        if (!containsKey(key)) throw Exception("Key $key does not exist in $this.")
        throw Exception("$key exists, but is not a dict: $this.")
    }
    return null
}


/**
 * Formats a double with max `maxDecimals` decimals. As long as the least significant decimal positions are 0, they
 * will be removed from the output. So `23.4567.formattedString(3)` outputs "23.457", but `23.0.formattedString(3)`
 * outputs "23".
 */
fun Double.formattedString(maxDecimals: Int, locale: Locale = Locale.getDefault()): String {
    val pattern =
        if (maxDecimals > 0) "0." + "0".repeat(maxDecimals)
        else "0"
    val symbols = DecimalFormatSymbols(locale)
    val decimalFormat = DecimalFormat(pattern, symbols)
    decimalFormat.isDecimalSeparatorAlwaysShown = false
    return decimalFormat.format(this).trimEnd('0').trimEnd(symbols.decimalSeparator)
}


@Suppress("unused")
fun getReadOnlyAudioCollection(): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI


fun getReadWriteAudioCollection(): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI


fun getReadOnlyImageCollection(): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI


fun getReadWriteImageCollection(): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI


fun File.deleteWithEmptyParentDirs(parentDirNames: List<String>) {
    if (isFile && canWrite()) {
        delete()
        var parentDir = parentFile

        for (dirName in parentDirNames) {
            if (
                parentDir?.isDirectory == true &&
                parentDir.name == dirName &&
                parentDir.list()?.isEmpty() == true &&
                parentDir.canWrite()
            ) {
                parentDir.delete()
                parentDir = parentDir.parentFile
            } else break
        }
    }
}


fun Context.deleteMediaStoreUriAndFile(collection: Uri, relativePath: String, filename: String) {
    val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
    val selectionArgs = arrayOf(filename, relativePath)

    getMediaStoreEntry(
        collection = collection,
        selection = selection,
        selectionArgs = selectionArgs,
    )?.also { entry ->
        contentResolver.delete(entry.uri, null, null)
        entry.file.deleteWithEmptyParentDirs(entry.parentDirNames)
    }
}


fun Context.deleteMediaStoreUriAndFile(collection: Uri, pathAndFilename: Pair<String, String>) =
    deleteMediaStoreUriAndFile(collection, pathAndFilename.first, pathAndFilename.second)


fun Long.bytesToString(): String {
    if (this < 2.0.pow(10)) return "$this B"
    if (this < 2.0.pow(20)) return "${this.div(2.0.pow(10)).formattedString(1)} KB"
    if (this < 2.0.pow(30)) return "${this.div(2.0.pow(20)).formattedString(1)} MB"
    if (this < 2.0.pow(40)) return "${this.div(2.0.pow(30)).formattedString(1)} GB"
    return "${this.div(2.0.pow(40)).formattedString(1)} TB"
}


fun MediaFormat.getIntegerOrNull(name: String): Int? = try {
    getInteger(name)
} catch (_: NullPointerException) {
    null
} catch (_: ClassCastException) {
    null
}

fun MediaFormat.getIntegerOrDefault(name: String, default: Int?): Int? = getIntegerOrNull(name) ?: default


/**
 * @throws FileNotFoundException
 */
fun Context.getMediaStoreFile(uri: Uri): File {
    contentResolver.query(uri, arrayOf(MediaColumns.DATA), null, null)?.use { cursor ->
        if (cursor.moveToNext()) {
            val filename = cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.DATA))
            return File(filename)
        }
    }
    throw FileNotFoundException(uri.path)
}


fun Context.getMediaStoreFileNullable(uri: Uri): File? = try {
    getMediaStoreFile(uri)
} catch (_: FileNotFoundException) {
    null
}


fun JSONObject.getStringOrNull(name: String): String? = if (has(name)) getString(name) else null


fun JSONObject.getIntOrNull(name: String): Int? =
    if (has(name)) getStringOrNull(name)?.takeWhile { it.isDigit() }?.takeIf { it.isNotEmpty() }?.toInt() else null


fun JSONObject.getDoubleOrNull(name: String): Double? = if (has(name)) getDouble(name) else null


fun Context.dpToPx(dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()


fun File.isImage(): Boolean =
    isFile && MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.startsWith("image/") == true


fun Bitmap.size() = width * height


fun Bitmap.scaleToMaxSize(maxSizeDp: Dp, context: Context): Bitmap {
    val maxSize = context.dpToPx(maxSizeDp.value.toInt())
    return if (width > maxSize || height > maxSize) {
        val scaleBy = maxSize.toDouble() / max(width, height)
        scale((width * scaleBy).toInt(), (height * scaleBy).toInt())
    } else this
}


fun String.substringMax(startIndex: Int, endIndex: Int) = substring(startIndex, kotlin.math.min(endIndex, length))


inline fun <T> Iterable<T>.sumOfOrNull(selector: (T) -> Long?): Long? {
    /**
     * Variation of sumOf(), which returns null if there are no elements in the
     * iterable for which `selector` returns non-null.
     */
    var sum: Long? = null
    for (element in this) {
        selector(element)?.also {
            sum = sum?.plus(it) ?: it
        }
    }
    return sum
}
