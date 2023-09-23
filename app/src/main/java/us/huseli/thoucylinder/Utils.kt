package us.huseli.thoucylinder

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.arthenica.ffmpegkit.FFprobeKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.thoucylinder.Constants.URL_CONNECT_TIMEOUT
import us.huseli.thoucylinder.Constants.URL_READ_TIMEOUT
import us.huseli.thoucylinder.dataclasses.TrackMetadata
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


fun String.sanitizeFilename(): String =
    replace(Regex("[/\\\\?%*:|\"<>\\x7F\\x00-\\x1F]"), "-")


fun String.toDuration(): Duration {
    try {
        return Duration.parseIsoString(this)
    } catch (_: IllegalArgumentException) {
    }

    val regex = Regex("(?:(?<hours>\\d+):)?(?<minutes>\\d+):(?<seconds>\\d+)$")
    val groups = regex.find(this)?.groups
    var duration = Duration.ZERO

    groups?.get("hours")?.value?.toInt()?.hours?.let { duration += it }
    groups?.get("minutes")?.value?.toInt()?.minutes?.let { duration += it }
    groups?.get("seconds")?.value?.toInt()?.seconds?.let { duration += it }
    return duration
}


inline fun <T> List<T>.combineEquals(predicate: (a: T, b: T) -> Boolean): List<List<T>> {
    val result = mutableListOf<List<T>>()
    val usedIndices = mutableListOf<Int>()

    forEachIndexed { leftIdx, left ->
        if (!usedIndices.contains(leftIdx)) {
            val list = mutableListOf(left)
            usedIndices.add(leftIdx)
            forEachIndexed { rightIdx, right ->
                if (!usedIndices.contains(rightIdx) && predicate(left, right)) {
                    list.add(right)
                    usedIndices.add(rightIdx)
                }
            }
            result.add(list)
        }
    }
    return result
}


inline fun <T, O> List<T>.zipBy(other: List<O>, predicate: (a: T, b: O) -> Boolean): List<Pair<T, O>> =
    mapNotNull { item ->
        other.find { predicate(item, it) }?.let { Pair(item, it) }
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


fun <T> List<T>.circular(offset: Int, length: Int): List<T> {
    val realOffset = offset % size
    if (realOffset + length > size)
        return subList(realOffset, size) + circular(0, length - size + realOffset)
    return subList(realOffset, realOffset + length)
}


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


fun File.toBitmap(): Bitmap? = takeIf { it.isFile }?.inputStream().use { BitmapFactory.decodeStream(it) }


fun getAudioCollection(): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI


fun deleteExistingMediaFile(context: Context, filename: String, subdirName: String?) {
    val localMusicDir: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).apply { mkdirs() }
    val audioCollection = getAudioCollection()
    val subdir = subdirName?.takeIf { it.isNotEmpty() }?.let { File(localMusicDir, subdirName) } ?: localMusicDir
    val dirName = "${Environment.DIRECTORY_MUSIC}/" + if (subdirName?.isNotEmpty() == true) "$subdirName/" else ""
    val projection = arrayOf(MediaStore.Audio.Media._ID)
    val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
    val selectionArgs = arrayOf(filename, dirName)

    File(subdir, filename).delete()
    if (subdirName?.isNotEmpty() == true && subdir.list()?.isEmpty() == true)
        subdir.delete()

    context.contentResolver.query(audioCollection, projection, selection, selectionArgs, null)?.use { cursor ->
        if (cursor.moveToNext()) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
            )
            context.contentResolver.delete(uri, null, null)
        }
    }
}


/**
 * Extract metadata from audio file with MediaExtractor and ffmpeg.
 * @throws ExtractTrackDataException
 */
fun File.extrackTrackMetadata(): TrackMetadata {
    val extractor = MediaExtractor()
    extractor.setDataSource(path)

    for (trackIdx in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(trackIdx)
        val mimeType = format.getString(MediaFormat.KEY_MIME)

        if (mimeType?.startsWith("audio/") == true) {
            val ff = FFprobeKit.getMediaInformation(path)?.mediaInformation
            val ffStream = ff?.streams?.getOrNull(trackIdx)
            val extension =
                when {
                    ffStream?.codec != null && ff.format?.contains(",") == true -> ffStream.codec
                    ff?.format != null -> ff.format
                    else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        ?: mimeType.split("/").last().lowercase()
                }
            val bitrate =
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) format.getInteger(MediaFormat.KEY_BIT_RATE)
                else ff?.bitrate?.toInt()
            val channels =
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                else ffStream?.getNumberProperty("channels")?.toInt()
            val sampleRate =
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                else ffStream?.sampleRate?.toInt()

            return TrackMetadata(
                bitrate = bitrate,
                durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000,
                extension = extension,
                mimeType = mimeType,
                sampleRate = sampleRate,
                channels = channels,
                size = length(),
            ).also { extractor.release() }
        }
    }
    extractor.release()
    throw ExtractTrackDataException(this, extractor)
}
