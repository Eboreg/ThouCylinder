package us.huseli.thoucylinder

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

object Umlautify {
    private const val RATIO: Double = 0.3
    private val _isEnabled = MutableStateFlow(false)
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

    val enabledUmlautifier: (CharSequence) -> String = { transform(string = it, force = true) }
    val disabledUmlautifier: (CharSequence) -> String = { it.toString() }
    val isEnabled = _isEnabled.asStateFlow()
    val umlautifier = _isEnabled.map { if (it) enabledUmlautifier else disabledUmlautifier }

    fun setEnabled(value: Boolean) {
        _isEnabled.value = value
    }

    fun transform(string: CharSequence, force: Boolean = false): String {
        return if (_isEnabled.value || force) {
            val random = Random(string.hashCode())

            string.replace(regex) {
                if (random.nextDouble() < RATIO) map[it.value]!!
                else it.value
            }
        } else string.toString()
    }
}

val LocalUmlautifier = staticCompositionLocalOf<(CharSequence) -> String> { Umlautify.disabledUmlautifier }

fun CharSequence.umlautify(): String = Umlautify.transform(this)

fun Context.getUmlautifiedString(@StringRes resId: Int, vararg formatArgs: Any?) =
    getString(resId, *formatArgs).umlautify()

@Composable
@ReadOnlyComposable
internal fun resources(): Resources {
    /**
     * This is how [androidx.compose.ui.res.resources()] does it, so I guess it must serve some purpose:
     */
    LocalConfiguration.current
    return LocalContext.current.resources
}

@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val umlautifier = LocalUmlautifier.current
    val resources = resources()

    return umlautifier(resources.getString(id, *formatArgs))
}

@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int): String {
    val umlautifier = LocalUmlautifier.current
    val resources = resources()

    return umlautifier(resources.getString(id))
}

@Composable
@ReadOnlyComposable
fun umlautStringResource(@StringRes id: Int): String {
    val resources = resources()

    return Umlautify.enabledUmlautifier(resources.getString(id))
}

@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String {
    val umlautifier = LocalUmlautifier.current
    val resources = resources()

    return umlautifier(resources.getQuantityString(id, count, *formatArgs))
}
