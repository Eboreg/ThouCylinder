package us.huseli.thoucylinder

import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject


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
