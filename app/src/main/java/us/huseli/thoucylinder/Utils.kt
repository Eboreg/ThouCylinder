package us.huseli.thoucylinder

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


fun sanitizeFilename(string: String): String =
    string.replace(Regex("[/\\\\?%*:|\"<>\\x7F\\x00-\\x1F]"), "-")


fun String.lengthToDuration(): Duration {
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


inline fun <T> List<T>.zipEquals(other: List<T>, predicate: (a: T, b: T) -> Boolean): List<Pair<T, T>> =
    mapNotNull { item ->
        other.find { predicate(item, it) }?.let { Pair(item, it) }
    }


fun <T> Map<*, *>.yquery(keys: String, failSilently: Boolean = true): T? {
    val splitKeys = keys.split(".", limit = 2)
    val key = splitKeys[0]
    val newKeys = splitKeys.getOrNull(1) ?: ""
    val value = this[key]

    if (newKeys.isEmpty()) {
        @Suppress("UNCHECKED_CAST")
        (value as? T)?.let { return it } ?: run { throw Exception("$value is not of correct type") }
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
