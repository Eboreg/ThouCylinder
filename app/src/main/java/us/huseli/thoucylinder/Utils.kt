package us.huseli.thoucylinder

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import android.util.Size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.huseli.retaintheme.ui.theme.RetainColorDark
import us.huseli.retaintheme.ui.theme.RetainColorLight
import us.huseli.retaintheme.ui.theme.RetainColorScheme
import us.huseli.thoucylinder.Constants.URL_CONNECT_TIMEOUT
import us.huseli.thoucylinder.Constants.URL_READ_TIMEOUT
import us.huseli.thoucylinder.dataclasses.getMediaStoreEntry
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLConnection
import java.util.Locale
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


suspend fun urlRequest(
    urlString: String,
    headers: Map<String, String> = emptyMap(),
    body: ByteArray? = null,
): URLConnection = withContext(Dispatchers.IO) {
    return@withContext URL(urlString).openConnection().apply {
        connectTimeout = URL_CONNECT_TIMEOUT
        readTimeout = URL_READ_TIMEOUT
        Log.i("Utils", "urlRequest: $urlString")
        headers.forEach { (key, value) -> setRequestProperty(key, value) }
        if (body != null) {
            doOutput = true
            getOutputStream().write(body, 0, body.size)
        }
    }
}


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


fun Context.deleteMediaStoreUriAndFile(filename: String, mediaStorePath: String?) {
    val audioCollection = getReadWriteAudioCollection()
    val dirName =
        "${Environment.DIRECTORY_MUSIC}/" + if (mediaStorePath?.isNotEmpty() == true) "$mediaStorePath/" else ""
    val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
    val selectionArgs = arrayOf(filename, dirName)

    getMediaStoreEntry(
        queryUri = audioCollection,
        selection = selection,
        selectionArgs = selectionArgs,
    )?.also { entry ->
        val contentUri = entry.getContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        contentResolver.delete(contentUri, null, null)
        entry.file.deleteWithEmptyParentDirs(entry.parentDirNames)
    }
}


val Context.thumbnailDir: File
    get() = File(filesDir, "thumbnails").apply { mkdirs() }


fun Long.bytesToString(): String {
    if (this < 2.0.pow(10)) return "$this B"
    if (this < 2.0.pow(20)) return "${this.div(2.0.pow(10)).formattedString(1)} KB"
    if (this < 2.0.pow(30)) return "${this.div(2.0.pow(20)).formattedString(1)} MB"
    if (this < 2.0.pow(40)) return "${this.div(2.0.pow(30)).formattedString(1)} GB"
    return "${this.div(2.0.pow(40)).formattedString(1)} TB"
}


@Composable
fun themeColors(): RetainColorScheme = if (isSystemInDarkTheme()) RetainColorDark else RetainColorLight


fun MediaFormat.getIntegerOrNull(name: String): Int? = try {
    getInteger(name)
} catch (_: NullPointerException) {
    null
} catch (_: ClassCastException) {
    null
}

fun MediaFormat.getIntegerOrDefault(name: String, default: Int?): Int? = getIntegerOrNull(name) ?: default


fun ContentResolver.loadThumbnailOrNull(uri: Uri, size: Size, signal: CancellationSignal?): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) try {
        loadThumbnail(uri, size, signal)
    } catch (_: FileNotFoundException) {
        null
    } else null


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


fun List<String>.leadingChars(): List<Char> =
    mapNotNull { string ->
        string.replace(Regex("[^\\w&&[^0-9]]"), "#").getOrNull(0)?.uppercaseChar()
    }.distinct().sorted()
