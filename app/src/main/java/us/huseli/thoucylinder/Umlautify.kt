package us.huseli.thoucylinder

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random

object Umlautify {
    var enabled: Boolean = false

    private const val ratio: Double = 0.3
    private val map = mapOf(
        "a" to "ä",
        "e" to "ë",
        "i" to "ï",
        "o" to "ö",
        "u" to "ü",
        "w" to "ẅ",
        "x" to "ẍ",
        "y" to "ÿ",
        "A" to "Ä",
        "E" to "Ë",
        "I" to "Ï",
        "O" to "Ö",
        "U" to "Ü",
        "W" to "Ẅ",
        "X" to "Ẍ",
        "Y" to "Ÿ",
    )
    private val regex = Regex("[${map.keys.joinToString("")}]")

    fun transform(string: String, force: Boolean = false): String {
        return if (enabled || force) {
            val random = Random(string.hashCode())

            string.replace(regex) {
                if (random.nextDouble() < ratio) map[it.value]!!
                else it.value
            }
        } else string
    }
}


@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val string =
        if (formatArgs.isNotEmpty()) LocalContext.current.resources.getString(id, *formatArgs)
        else LocalContext.current.resources.getString(id)
    return if (Umlautify.enabled) string.umlautify() else string
}


@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String {
    val string =
        if (formatArgs.isNotEmpty()) LocalContext.current.resources.getQuantityString(id, count, *formatArgs)
        else LocalContext.current.resources.getQuantityString(id, count)
    return if (Umlautify.enabled) string.umlautify() else string
}


fun String.umlautify(): String = Umlautify.transform(this)
