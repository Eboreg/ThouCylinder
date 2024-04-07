package us.huseli.thoucylinder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.file.fullName
import com.anggrayudi.storage.file.isWritable
import com.anggrayudi.storage.file.mimeType
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.openOutputStream
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.pow
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.square
import us.huseli.retaintheme.extensions.toBitmap
import us.huseli.thoucylinder.Constants.IMAGE_FULL_MAX_WIDTH_DP
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_DP
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Logger : ILogger

@Suppress("UNCHECKED_CAST")
fun Map<*, *>.yqueryString(key: String, failSilently: Boolean = true): String? {
    val value = yquery<Any>(key, failSilently)

    return try {
        val castValue =
            if (value is Map<*, *>)
                (value["runs"] as? Collection<Map<*, *>>)?.firstOrNull()?.get("text") as? String
                    ?: value["simpleText"] as? String
            else value as? String

        if (castValue == null && !failSilently) throw Exception("$value is not of correct type")
        castValue
    } catch (e: ClassCastException) {
        if (!failSilently) throw Exception("$value is not of correct type")
        null
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
            if (castValue == null && !failSilently) throw Exception("$value is not of correct type")
            castValue
        } catch (e: ClassCastException) {
            if (!failSilently) throw Exception("$value is not of correct type")
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

fun ViewModel.launchOnIOThread(block: suspend CoroutineScope.() -> Unit) =
    viewModelScope.launch(Dispatchers.IO, block = block)

fun ViewModel.launchOnMainThread(block: suspend CoroutineScope.() -> Unit) =
    viewModelScope.launch(Dispatchers.Main, block = block)

val Context.imageCacheDir: File
    get() = File(cacheDir, "images").apply { mkdirs() }


/** STRING ********************************************************************/
fun String.escapeQuotes() = replace("\"", "\\\"")

suspend fun String.getBitmapByUrl(): Bitmap? = withContext(Dispatchers.IO) {
    try {
        Request(this@getBitmapByUrl).getBitmap()
    } catch (e: HTTPResponseError) {
        Logger.logError("String", "getBitmapByUrl: $e", e)
        null
    } catch (e: IOException) {
        Logger.logError("String", "getBitmapByUrl: $e", e)
        null
    }
}

val gson: Gson = GsonBuilder().create()

fun <T> String.fromJson(typeOfT: TypeToken<T>): T = gson.fromJson(this, typeOfT)

/** "Should not be used if the desired type is a generic type." */
inline fun <reified T> String.fromJson(): T = gson.fromJson(this, T::class.java)


/** MEDIAFORMAT ***************************************************************/
fun MediaFormat.getIntegerOrNull(name: String): Int? = try {
    getInteger(name)
} catch (e: NullPointerException) {
    Logger.logError("MediaFormat", "getIntegerOrNull: $e", e)
    null
} catch (e: ClassCastException) {
    Logger.logError("MediaFormat", "getIntegerOrNull: $e", e)
    null
}

fun MediaFormat.getLongOrNull(name: String): Long? = try {
    getLong(name)
} catch (e: NullPointerException) {
    Logger.logError("MediaFormat", "getLongOrNull: $e", e)
    null
} catch (e: ClassCastException) {
    Logger.logError("MediaFormat", "getLongOrNull: $e", e)
    null
}

fun MediaFormat.getIntegerOrDefault(name: String, default: Int?): Int? = getIntegerOrNull(name) ?: default


/** JSONOBJECT ****************************************************************/
fun JSONObject.getStringOrNull(name: String): String? = if (has(name)) getString(name) else null

fun JSONObject.getIntOrNull(name: String): Int? =
    if (has(name)) getStringOrNull(name)?.takeWhile { it.isDigit() }?.toIntOrNull() else null


/** BITMAP/IMAGEBITMAP ********************************************************/
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

fun Bitmap.getSquareSize() = min(width, height).pow(2)

fun Bitmap.asThumbnailImageBitmap(context: Context): ImageBitmap =
    square().scaleToMaxSize(IMAGE_THUMBNAIL_MAX_WIDTH_DP.dp, context).asImageBitmap()


/** URI ***********************************************************************/
fun Uri.getRelativePath(): String? = lastPathSegment?.substringAfterLast(':')?.nullIfEmpty()

fun Uri.getRelativePathWithoutFilename(): String? = getRelativePath()?.substringBeforeLast('/')?.nullIfEmpty()

suspend fun Uri.getBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (this@getBitmap.isRemote) {
            Request(this@getBitmap.toString()).getBitmap()
        } else {
            this@getBitmap.openInputStream(context)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: HTTPResponseError) {
        Logger.logError("Uri", "getBitmap: $e", e)
        null
    } catch (e: IOException) {
        Logger.logError("Uri", "getBitmap: $e", e)
        null
    }
}

val Uri.isRemote: Boolean
    // Very primitive and incomplete check, but should be good enough for us.
    get() = scheme?.matches(Regex("^(https?)|(ftps?)$", RegexOption.IGNORE_CASE)) == true

private suspend fun Uri.getCachedBitmap(context: Context, thumbnail: Boolean): Bitmap? {
    return withContext(Dispatchers.IO) {
        val size = if (thumbnail) IMAGE_THUMBNAIL_MAX_WIDTH_DP.dp else IMAGE_FULL_MAX_WIDTH_DP.dp

        if (isRemote) {
            val cacheFilename =
                this@getCachedBitmap.hashCode().toString() + (if (thumbnail) "-thumbnail.jpg" else "-full.jpg")
            val cacheFile = File(context.imageCacheDir, cacheFilename)

            if (!cacheFile.exists()) {
                getBitmap(context)?.square()?.scaleToMaxSize(size, context)?.also { bitmap ->
                    cacheFile.outputStream().use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    }
                }
            }
            cacheFile.toBitmap()
        } else {
            getBitmap(context)?.square()?.scaleToMaxSize(size, context)
        }
    }
}

suspend fun Uri.getCachedFullBitmap(context: Context) = getCachedBitmap(context, false)

suspend fun Uri.getCachedThumbnailBitmap(context: Context) = getCachedBitmap(context, true)


/** DOCUMENTFILE **************************************************************/
@WorkerThread
fun DocumentFile.toByteArray(context: Context): ByteArray? = try {
    openInputStream(context)?.use { it.readBytes() }
} catch (e: FileNotFoundException) {
    Logger.logError("DocumentFile", "toByteArray: $e", e)
    null
} catch (e: IllegalArgumentException) {
    Logger.logError("DocumentFile", "toByteArray: $e", e)
    null
}

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
    File(directory, filename).also { outfile -> copyTo(outfile, context) }

@WorkerThread
fun DocumentFile.copyTo(outFile: File, context: Context) {
    outFile.outputStream().use { outputStream ->
        openInputStream(context)?.use { inputStream ->
            outputStream.write(inputStream.readBytes())
        }
    }
}

@WorkerThread
fun DocumentFile.copyFrom(inFile: File, context: Context) {
    openOutputStream(context, append = false)?.use { outputStream ->
        inFile.inputStream().use { inputStream ->
            outputStream.write(inputStream.readBytes())
        }
    }
}
