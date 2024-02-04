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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.file.fullName
import com.anggrayudi.storage.file.isWritable
import com.anggrayudi.storage.file.mimeType
import com.anggrayudi.storage.file.openInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


fun <T> List<T>.slice(start: Int, maxCount: Int) =
    if (start >= size) emptyList()
    else subList(start, min(start + maxCount, size))


fun Color.distance(other: Color): Float {
    val drp2 = (red - other.red).pow(2)
    val dgp2 = (green - other.green).pow(2)
    val dbp2 = (blue - other.blue).pow(2)
    val t = (red + other.red) / 2

    return sqrt(2 * drp2 + 4 * dgp2 + 3 * dbp2 + t * (drp2 - dbp2) / 256)
}


fun Int.pow(n: Int): Int = toDouble().pow(n).toInt()


fun Bitmap.square(): Bitmap {
    val length = min(width, height)

    return if (width == height) this
    else Bitmap.createBitmap(this, (width - length) / 2, (height - length) / 2, length, length)
}


@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<K, V?>.filterValuesNotNull(): Map<K, V> = filterValues { it != null } as Map<K, V>


fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }


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

suspend fun String.getSquareBitmapByUrl(): Bitmap? = withContext(Dispatchers.IO) {
    try {
        Request.get(this@getSquareBitmapByUrl).connect().getSquareBitmap()
    } catch (e: FileNotFoundException) {
        Log.e("String", "getBitmapByUrl: $e", e)
        null
    } catch (e: IOException) {
        Log.e("String", "getBitmapByUrl: $e", e)
        null
    }
}


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

fun Bitmap.asFullImageBitmap(context: Context): ImageBitmap =
    scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context).asImageBitmap()

fun Bitmap.asThumbnailImageBitmap(context: Context): ImageBitmap =
    scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context).asImageBitmap()


/** URI ***********************************************************************/
fun Uri.getRelativePath() = lastPathSegment?.substringAfterLast(':')?.nullIfEmpty()

fun Uri.getRelativePathWithoutFilename() = getRelativePath()?.substringBeforeLast('/')?.nullIfEmpty()

suspend fun Uri.getSquareBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (this@getSquareBitmap.scheme?.startsWith("http") == true) {
            Request(this@getSquareBitmap.toString()).connect().getSquareBitmap()
        } else {
            this@getSquareBitmap.openInputStream(context)?.use { BitmapFactory.decodeStream(it).square() }
        }
    } catch (e: FileNotFoundException) {
        Log.e("Uri", "getSquareBitmap: $e", e)
        null
    } catch (e: IOException) {
        Log.e("Uri", "getSquareBitmap: $e", e)
        null
    }
}


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
fun DocumentFile.toByteArray(context: Context): ByteArray? = try {
    openInputStream(context)?.use { it.readBytes() }
} catch (e: FileNotFoundException) {
    Log.e("DocumentFile", "toByteArray: $e", e)
    null
} catch (e: IllegalArgumentException) {
    Log.e("DocumentFile", "toByteArray: $e", e)
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

@Suppress("unused")
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


@WorkerThread
fun DocumentFile.copyTo(context: Context, directory: File, filename: String = fullName): File =
    File(directory, filename).also { outfile ->
        outfile.outputStream().use { outputStream ->
            openInputStream(context)?.use { inputStream ->
                outputStream.write(inputStream.readBytes())
            }
        }
    }
