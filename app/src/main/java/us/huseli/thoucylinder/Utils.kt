package us.huseli.thoucylinder

import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt


fun getApiUserAgent() = "ThouCylinder/${BuildConfig.VERSION_NAME} ( https://github.com/Eboreg/ThouCylinder )"


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


fun MediaFormat.getIntegerOrNull(name: String): Int? = try {
    getInteger(name)
} catch (_: NullPointerException) {
    null
} catch (_: ClassCastException) {
    null
}

fun MediaFormat.getIntegerOrDefault(name: String, default: Int?): Int? = getIntegerOrNull(name) ?: default


fun JSONObject.getStringOrNull(name: String): String? = if (has(name)) getString(name) else null


fun JSONObject.getIntOrNull(name: String): Int? =
    if (has(name)) getStringOrNull(name)?.takeWhile { it.isDigit() }?.takeIf { it.isNotEmpty() }?.toInt() else null


fun JSONObject.getDoubleOrNull(name: String): Double? = if (has(name)) getDouble(name) else null


fun Long.toInstant(): Instant = Instant.ofEpochSecond(this)


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


fun <T> List<T>.clone(): MutableList<T> = mutableListOf<T>().also { it.addAll(this) }


enum class DateTimePrecision { DAY, HOUR, MINUTE, SECOND }


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
