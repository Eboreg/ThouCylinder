package us.huseli.thoucylinder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.isWritable
import com.anggrayudi.storage.file.mimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class DateTimePrecision { DAY, HOUR, MINUTE, SECOND }


fun getApiUserAgent() = "ThouCylinder/${BuildConfig.VERSION_NAME} ( https://github.com/Eboreg/ThouCylinder )"


fun <K, V> List<Map<K, V>>.join(): Map<K, V> =
    mutableMapOf<K, V>().also { map { map -> it.putAll(map) } }.toMap()


@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<K, V?>.filterValuesNotNull(): Map<K, V> = filterValues { it != null } as Map<K, V>


fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }


fun Long.toInstant(): Instant = Instant.ofEpochSecond(this)


fun <T> List<T>.clone(): MutableList<T> = mutableListOf<T>().also { it.addAll(this) }


fun <T> List<T>.padStart(length: Int, value: T? = null): List<T?> =
    if (size < length) List(length - size) { value }.plus(this)
    else this


fun <T> List<T>.listItemsBetween(item1: T, item2: T): List<T> {
    /** from & to are both exclusive */
    val item1Index = indexOf(item1)
    val item2Index = indexOf(item2)
    val fromIndex = min(item1Index, item2Index)
    val toIndex = max(item1Index, item2Index)

    return when {
        fromIndex == -1 || toIndex == -1 -> emptyList()
        toIndex - fromIndex < 2 -> emptyList()
        else -> subList(fromIndex + 1, toIndex)
    }
}


fun Instant.isoDateTime(precision: DateTimePrecision = DateTimePrecision.SECOND): String {
    val pattern = when (precision) {
        DateTimePrecision.DAY -> "yyyy-MM-dd"
        DateTimePrecision.HOUR -> "yyyy-MM-dd HH"
        DateTimePrecision.MINUTE -> "yyyy-MM-dd HH:mm"
        DateTimePrecision.SECOND -> "yyyy-MM-dd HH:mm:ss"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
    return formatter.format(this)
}


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


/** STRING ********************************************************************/

fun String.escapeQuotes() = replace("\"", "\\\"")

fun String.nullIfBlank(): String? = takeIf { it.isNotBlank() }

fun String.nullIfEmpty(): String? = takeIf { it.isNotEmpty() }

fun String.capitalized() = replace(Regex("((^\\p{L})|(?<=\\P{L})(\\p{L}))")) { it.value.uppercase() }

suspend fun String.getBitmapByUrl(): Bitmap? = withContext(Dispatchers.IO) {
    try {
        Request.get(this@getBitmapByUrl).connect().getBitmap()
    } catch (e: FileNotFoundException) {
        Log.e("String", "getBitmapByUrl: $e", e)
        null
    } catch (e: IOException) {
        Log.e("String", "getBitmapByUrl: $e", e)
        null
    }
}

fun String.md5(): ByteArray = MessageDigest.getInstance("MD5").digest(toByteArray(Charsets.UTF_8))


/** MEDIAFORMAT ***************************************************************/

fun MediaFormat.getIntegerOrNull(name: String): Int? = try {
    getInteger(name)
} catch (e: NullPointerException) {
    Log.e("MediaFormat", "getIntegerOrNull: $e", e)
    null
} catch (e: ClassCastException) {
    Log.e("MediaFormat", "getIntegerOrNull: $e", e)
    null
}

fun MediaFormat.getLongOrNull(name: String): Long? = try {
    getLong(name)
} catch (e: NullPointerException) {
    Log.e("MediaFormat", "getLongOrNull: $e", e)
    null
} catch (e: ClassCastException) {
    Log.e("MediaFormat", "getLongOrNull: $e", e)
    null
}

fun MediaFormat.getIntegerOrDefault(name: String, default: Int?): Int? = getIntegerOrNull(name) ?: default


/** JSONOBJECT ****************************************************************/

fun JSONObject.getStringOrNull(name: String): String? = if (has(name)) getString(name) else null

fun JSONObject.getIntOrNull(name: String): Int? =
    if (has(name)) getStringOrNull(name)?.takeWhile { it.isDigit() }?.toIntOrNull() else null

fun JSONObject.getDoubleOrNull(name: String): Double? = if (has(name)) getDouble(name) else null


/** IMAGEBITMAP ***************************************************************/

fun ImageBitmap.getAverageColor(): Color {
    val pixelMap = toPixelMap()
    var redSum = 0f
    var greenSum = 0f
    var blueSum = 0f
    val stepX = max((1f / (100f / pixelMap.width)).roundToInt(), 1)
    val stepY = max((1f / (100f / pixelMap.height)).roundToInt(), 1)
    var sampleCount = 0

    for (x in 0 until pixelMap.width step stepX) {
        for (y in 0 until pixelMap.height step stepY) {
            pixelMap[x, y].also {
                redSum += it.red
                greenSum += it.green
                blueSum += it.blue
            }
            sampleCount++
        }
    }

    return Color(
        red = redSum / sampleCount,
        green = greenSum / sampleCount,
        blue = blueSum / sampleCount,
    )
}


/** URI ***********************************************************************/

fun Uri.getRelativePath() = lastPathSegment?.substringAfterLast(':')?.nullIfEmpty()

fun Uri.getRelativePathWithoutFilename() = getRelativePath()?.substringBeforeLast('/')?.nullIfEmpty()


/** DOCUMENTFILE **************************************************************/

@WorkerThread
fun DocumentFile.toBitmap(context: Context): Bitmap? = try {
    context.contentResolver.openFileDescriptor(uri, "r")
        ?.use { BitmapFactory.decodeFileDescriptor(it.fileDescriptor) }
} catch (e: FileNotFoundException) {
    Log.e("DocumentFile", "toBitmap: $e", e)
    null
} catch (e: IllegalArgumentException) {
    Log.e("DocumentFile", "toBitmap: $e", e)
    null
}

@WorkerThread
fun DocumentFile.createDirectoryIfNotExists(displayName: String): DocumentFile? =
    findFile(displayName) ?: createDirectory(displayName)

@WorkerThread
fun DocumentFile.matchFiles(filename: Regex, mimeType: Regex? = null): List<DocumentFile> {
    return if (isDirectory) {
        listFiles().mapNotNull { documentFile ->
            if (
                documentFile.isFile &&
                documentFile.name?.matches(filename) == true &&
                (mimeType == null || documentFile.mimeType?.matches(mimeType) == true)
            ) documentFile
            else null
        }
    } else emptyList()
}

@WorkerThread
fun DocumentFile.matchFilesRecursive(filename: Regex, mimeType: Regex? = null): List<DocumentFile> {
    val matches = mutableListOf<DocumentFile>()

    if (isDirectory) {
        listFiles().forEach { documentFile ->
            if (documentFile.isFile && documentFile.name?.matches(filename) == true) {
                if (mimeType == null || documentFile.mimeType?.matches(mimeType) == true) matches.add(documentFile)
            } else if (documentFile.isDirectory) matches.addAll(documentFile.matchFilesRecursive(filename))
        }
    }
    return matches.distinctBy { it.uri.path }
}

@WorkerThread
fun DocumentFile.matchDirectoriesRecursive(dirname: Regex): List<DocumentFile> {
    val matches = mutableListOf<DocumentFile>()

    if (isDirectory) {
        if (name?.matches(dirname) == true) matches.add(this)
        listFiles().forEach { documentFile ->
            if (documentFile.isDirectory) matches.addAll(documentFile.matchDirectoriesRecursive(dirname))
        }
    }
    return matches.distinctBy { it.uri.path }
}

@WorkerThread
fun DocumentFile.getParentDirectory(context: Context): DocumentFile? =
    uri.lastPathSegment?.split('/')?.dropLast(1)?.let { lastPathSegments ->
        val parentUri = uri.buildUpon()
            .path(uri.pathSegments.dropLast(1).joinToString("/"))
            .appendPath(lastPathSegments.joinToString("/"))
            .build()
        DocumentFile.fromTreeUri(context, parentUri)
    }


@WorkerThread
fun DocumentFile.deleteWithEmptyParentDirs(context: Context) {
    if (isFile && isWritable(context)) {
        var parentDocumentFile: DocumentFile? = getParentDirectory(context)

        delete()

        while (
            parentDocumentFile != null &&
            parentDocumentFile.canWrite() &&
            parentDocumentFile.listFiles().isEmpty()
        ) {
            parentDocumentFile.delete()
            parentDocumentFile = parentDocumentFile.getParentDirectory(context)
        }
    }
}
