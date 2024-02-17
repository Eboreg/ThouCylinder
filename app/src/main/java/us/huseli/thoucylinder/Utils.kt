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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.pow
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.square
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_FULL
import us.huseli.thoucylinder.Constants.IMAGE_MAX_DP_THUMBNAIL
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

suspend fun String.getBitmapByUrl(): Bitmap? = withContext(Dispatchers.IO) {
    try {
        Request(this@getBitmapByUrl).getBitmap()
    } catch (e: FileNotFoundException) {
        Log.e("String", "getBitmapByUrl: $e", e)
        null
    } catch (e: IOException) {
        Log.e("String", "getBitmapByUrl: $e", e)
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

fun ImageBitmap.getSquareSize() = min(width, height).pow(2)

fun Bitmap.asFullImageBitmap(context: Context): ImageBitmap =
    square().scaleToMaxSize(IMAGE_MAX_DP_FULL.dp, context).asImageBitmap()

fun Bitmap.asThumbnailImageBitmap(context: Context): ImageBitmap =
    square().scaleToMaxSize(IMAGE_MAX_DP_THUMBNAIL.dp, context).asImageBitmap()


/** URI ***********************************************************************/
fun Uri.getRelativePath(): String? = lastPathSegment?.substringAfterLast(':')?.nullIfEmpty()

fun Uri.getRelativePathWithoutFilename(): String? = getRelativePath()?.substringBeforeLast('/')?.nullIfEmpty()

suspend fun Uri.getBitmap(context: Context): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (this@getBitmap.scheme?.startsWith("http") == true) {
            Request(this@getBitmap.toString()).getBitmap()
        } else {
            this@getBitmap.openInputStream(context)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: FileNotFoundException) {
        Log.e("Uri", "getBitmap: $e", e)
        null
    } catch (e: IOException) {
        Log.e("Uri", "getBitmap: $e", e)
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
