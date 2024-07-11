package us.huseli.thoucylinder

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.file.fullName
import com.anggrayudi.storage.file.isWritable
import com.anggrayudi.storage.file.mimeType
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.openOutputStream
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import us.huseli.retaintheme.extensions.formattedString
import us.huseli.retaintheme.extensions.nullIfEmpty
import us.huseli.retaintheme.extensions.pow
import us.huseli.retaintheme.extensions.scaleToMaxSize
import us.huseli.retaintheme.extensions.square
import us.huseli.retaintheme.extensions.toBitmap
import us.huseli.thoucylinder.Constants.IMAGE_FULL_MAX_WIDTH_DP
import us.huseli.thoucylinder.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_DP
import us.huseli.thoucylinder.dataclasses.artist.IArtist
import us.huseli.thoucylinder.dataclasses.artist.IArtistCredit
import us.huseli.thoucylinder.dataclasses.artist.ISavedArtist
import us.huseli.thoucylinder.interfaces.ILogger
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale
import kotlin.math.absoluteValue
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

val Context.imageCacheDir: File
    get() = File(cacheDir, "images").apply { mkdirs() }

fun <T> List<T>.offset(offset: Int): List<T> {
    val startIdx =
        if (offset < 0) lastIndex - ((offset.absoluteValue - 1) % size)
        else offset % size

    return subList(startIdx, size) + subList(0, startIdx)
}

fun waveList(length: Int, min: Int, max: Int, repeatEach: Int = 1, offset: Int = 0): List<Int> {
    if (min >= max) throw Exception("waveList: min ($min) >= max ($max)")

    val list = mutableListOf<Int>()
    val range = ((min until max) + (max downTo min + 1)).offset(offset)

    while (list.size < length) {
        for (value in range) {
            for (rep in 1..repeatEach) {
                list.add(value)
                if (list.size == length) break
            }
            if (list.size == length) break
        }
    }

    return list.toList()
}

fun Float.formattedString(maxDecimals: Int, locale: Locale = Locale.getDefault()): String =
    toDouble().formattedString(maxDecimals, locale)


inline fun AnnotatedString.Builder.addClickableArtist(
    artist: IArtist,
    crossinline onArtistClick: (String) -> Unit,
    spanStyle: SpanStyle = SpanStyle(),
    prefix: String? = null,
    postfix: String? = null,
) {
    val artistId = if (artist is ISavedArtist) artist.artistId else null

    if (prefix != null) append(prefix.umlautify())
    if (artistId != null) appendLink(
        text = artist.name,
        spanStyle = spanStyle,
        onClick = { onArtistClick(artistId) },
    )
    else withStyle(spanStyle) { append(artist.name.umlautify()) }
    if (postfix != null) append(postfix.umlautify())
}


inline fun AnnotatedString.Builder.appendLink(
    text: String,
    spanStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    crossinline onClick: () -> Unit,
) {
    pushLink(
        LinkAnnotation.Clickable(
            tag = text,
            linkInteractionListener = { onClick() },
        )
    )
    withStyle(spanStyle) { append(text.umlautify()) }
    pop()
}


inline fun getClickableArtist(
    artist: IArtist,
    crossinline onArtistClick: (String) -> Unit,
    spanStyle: SpanStyle = SpanStyle(),
    prefix: String? = null,
    postfix: String? = null,
): AnnotatedString = buildAnnotatedString {
    addClickableArtist(
        artist = artist,
        onArtistClick = onArtistClick,
        prefix = prefix,
        postfix = postfix,
        spanStyle = spanStyle,
    )
}


inline fun getClickableArtists(
    artists: ImmutableList<IArtistCredit>,
    spanStyle: SpanStyle = SpanStyle(),
    crossinline onArtistClick: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    artists.sortedBy { it.position }.forEachIndexed { index, artist ->
        addClickableArtist(
            artist = artist,
            onArtistClick = onArtistClick,
            postfix = if (index < artists.size - 1) artist.joinPhrase else null,
            spanStyle = spanStyle,
        )
    }
}

inline fun <T, K> Iterable<T>.sortedLike(other: Iterable<T>, crossinline key: (T) -> K): List<T> =
    sortedBy { item -> other.indexOfFirst { key(it) == key(item) } }

fun <T> Iterable<T>.sortedLike(other: Iterable<T>) = sortedLike(other, key = { it })

@Composable
fun isInLandscapeMode(): Boolean {
    val configuration = LocalConfiguration.current

    return remember { configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
}

inline fun <T> List<T>.listItemsBetween(item1: T, item2: T, key: (T) -> Any?): List<T> {
    /** from & to are both exclusive */
    val keyList = map { key(it) }
    val item1Index = keyList.indexOf(key(item1))
    val item2Index = keyList.indexOf(key(item2))
    val fromIndex = min(item1Index, item2Index)
    val toIndex = max(item1Index, item2Index)

    return when {
        fromIndex == -1 || toIndex == -1 -> emptyList()
        toIndex - fromIndex < 2 -> emptyList()
        else -> subList(fromIndex + 1, toIndex)
    }
}

fun <T> List<T>.listItemsBetween(item1: T, item2: T): List<T> =
    listItemsBetween(item1 = item1, item2 = item2, key = { it })

fun <T> List<T>.nextOrFirst(current: T): T {
    val currentIdx = indexOf(current)

    if (isEmpty()) throw Exception("nextOrFirst() needs at least 1 element")
    if (currentIdx == -1 || currentIdx == lastIndex) return this[0]
    return this[currentIdx + 1]
}

fun Collection<String>.stripCommonFixes(): Collection<String> {
    /** Strip prefixes and suffixes that are shared among all the strings. */
    if (size < 2) return this

    val firstString = first()
    val firstReversed = firstString.reversed()
    var prefix = ""
    var suffix = ""

    for (charPos in firstString.indices) {
        if (all { it.getOrNull(charPos) == firstString[charPos] }) prefix += firstString[charPos]
        else break
    }
    for (charPos in firstReversed.indices) {
        if (all { it.reversed().getOrNull(charPos) == firstReversed[charPos] }) suffix = firstReversed[charPos] + suffix
        else break
    }
    return map { it.removePrefix(prefix).removeSuffix(suffix) }
}

fun <K, V> Map<out K, V>.take(limit: Int): Map<K, V> = toList().take(limit).toMap()


/** STRING ************************************************************************************************************/

fun String.escapeQuotes() = replace("\"", "\\\"")

val gson: Gson = GsonBuilder().create()

fun <T> String.fromJson(typeOfT: TypeToken<T>): T = gson.fromJson(this, typeOfT)

/** "Should not be used if the desired type is a generic type." */
inline fun <reified T> String.fromJson(): T = gson.fromJson(this, T::class.java)

fun String.stripArtist(artist: String?): String =
    if (artist == null) this
    else replace(Regex("^\\W*$artist\\W+(.*)$", RegexOption.IGNORE_CASE), "$1")


/** MEDIAFORMAT *******************************************************************************************************/

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


/** JSONOBJECT ********************************************************************************************************/

fun JSONObject.getStringOrNull(name: String): String? = if (has(name)) getString(name) else null

fun JSONObject.getIntOrNull(name: String): Int? =
    if (has(name)) getStringOrNull(name)?.takeWhile { it.isDigit() }?.toIntOrNull() else null


/** BITMAP/IMAGEBITMAP ************************************************************************************************/

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


/** URI ***************************************************************************************************************/

fun Uri.getRelativePath(): String? = lastPathSegment?.substringAfterLast(':')?.nullIfEmpty()

suspend fun Uri.getBitmap(context: Context? = null): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (this@getBitmap.isRemote) {
            Request(this@getBitmap.toString()).getBitmap()
        } else if (context != null) {
            this@getBitmap.openInputStream(context)?.use { BitmapFactory.decodeStream(it) }
        } else {
            path?.let { FileInputStream(File(it)) }?.use { BitmapFactory.decodeStream(it) }
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

private suspend fun Uri.getBitmap(context: Context, thumbnail: Boolean, saveToCache: Boolean): Bitmap? {
    val size = if (thumbnail) IMAGE_THUMBNAIL_MAX_WIDTH_DP.dp else IMAGE_FULL_MAX_WIDTH_DP.dp
    val cacheFilename = hashCode().toString() + (if (thumbnail) "-thumbnail.jpg" else "-full.jpg")
    val cacheFile = File(context.imageCacheDir, cacheFilename)

    if (isRemote && cacheFile.exists()) {
        cacheFile.toBitmap()?.also { return it }
        cacheFile.delete()
    }

    return getBitmap(context)?.square()?.scaleToMaxSize(size, context)?.also { bitmap ->
        if (saveToCache && isRemote) cacheFile.outputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
    }
}

suspend fun Uri.getFullBitmap(context: Context, saveToCache: Boolean): Bitmap? =
    getBitmap(context, false, saveToCache)


/** DOCUMENTFILE ******************************************************************************************************/

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

@WorkerThread
fun DocumentFile.matchFilesRecursive(filename: Regex, mimeType: Regex? = null): List<DocumentFile> {
    // TODO Was used for finding cover.* I think, maybe want to to that again.
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
