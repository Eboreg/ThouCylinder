package us.huseli.thoucylinder

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

object Umlautify {
    private const val RATIO: Double = 0.3
    private val enabled = MutableStateFlow(false)
    private val map = persistentMapOf(
        "a" to "ä",
        "e" to "ë",
        "i" to "ï",
        "ı" to "ï",
        "o" to "ö",
        "u" to "ü",
        "w" to "ẅ",
        "x" to "ẍ",
        "y" to "ÿ",
        "A" to "Ä",
        "E" to "Ë",
        "I" to "Ï",
        "İ" to "Ï",
        "O" to "Ö",
        "U" to "Ü",
        "W" to "Ẅ",
        "X" to "Ẍ",
        "Y" to "Ÿ",
    )
    private val regex = Regex("[${map.keys.joinToString("")}]")

    val isEnabled: Boolean
        get() = enabled.value

    fun setEnabled(value: Boolean) {
        enabled.value = value
    }

    fun transform(string: String, force: Boolean = false): String {
        return if (enabled.value || force) {
            val random = Random(string.hashCode())

            string.replace(regex) {
                if (random.nextDouble() < RATIO) map[it.value]!!
                else it.value
            }
        } else string
    }
}


@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val string = LocalContext.current.resources.getString(id, *formatArgs)
    return if (Umlautify.isEnabled) string.umlautify(force = true) else string
}


@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int): String {
    val string = LocalContext.current.resources.getString(id)
    return if (Umlautify.isEnabled) string.umlautify(force = true) else string
}


@Composable
@ReadOnlyComposable
fun umlautStringResource(@StringRes id: Int): String =
    LocalContext.current.resources.getString(id).umlautify(force = true)


@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String {
    val string = LocalContext.current.resources.getQuantityString(id, count, *formatArgs)
    return if (Umlautify.isEnabled) string.umlautify(force = true) else string
}


fun String.umlautify(force: Boolean = false): String = Umlautify.transform(string = this, force = force)
