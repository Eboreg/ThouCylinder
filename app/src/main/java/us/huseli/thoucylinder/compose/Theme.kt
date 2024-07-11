package us.huseli.thoucylinder.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.ui.theme.DarkColors
import us.huseli.retaintheme.ui.theme.LightColors
import us.huseli.retaintheme.ui.theme.RetainTheme
import us.huseli.retaintheme.ui.theme.getColorScheme

@Immutable
data class BodyTextStyles(
    val primary: TextStyle = TextStyle.Default,
    val primaryBold: TextStyle = TextStyle.Default,
    val primarySmall: TextStyle = TextStyle.Default,
    val primarySmallBold: TextStyle = TextStyle.Default,
    val primaryExtraSmall: TextStyle = TextStyle.Default,
    val secondary: TextStyle = TextStyle.Default,
    val secondaryBold: TextStyle = TextStyle.Default,
    val secondarySmall: TextStyle = TextStyle.Default,
    val secondarySmallBold: TextStyle = TextStyle.Default,
    val secondaryExtraSmall: TextStyle = TextStyle.Default,
)

@Immutable
data class ThemeSizes(
    val largerIconButton: Dp = 48.dp,
    val largerIconButtonIcon: Dp = 32.dp,
)

val LocalBodyTextStyles = staticCompositionLocalOf { BodyTextStyles() }
val LocalTypography = staticCompositionLocalOf { Typography() }
val LocalThemeSizes = staticCompositionLocalOf { ThemeSizes() }

object FistopyTheme {
    val bodyStyles: BodyTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalBodyTextStyles.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}

@Composable
fun FistopyTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkColors = DarkColors.copy(
        background = DarkColors.surface,
        tertiary = Color(255, 86, 86),
        tertiaryContainer = Color(113, 0, 0),
    )
    val lightColors = LightColors.copy(
        background = LightColors.surface,
        tertiary = Color(133, 19, 19),
        tertiaryContainer = Color(255, 163, 163),
    )
    val colorScheme = getColorScheme(
        lightColors = lightColors,
        darkColors = darkColors,
        dynamicColor = dynamicColor,
        useDarkTheme = useDarkTheme,
    )
    val typography = Typography()
    val themeSizes = ThemeSizes()

    val bodyTextStyles = BodyTextStyles(
        primary = typography.bodyLarge,
        primaryBold = typography.titleMedium,
        primarySmall = typography.bodyMedium,
        primarySmallBold = typography.titleSmall,
        primaryExtraSmall = typography.bodySmall,
        secondary = typography.bodyLarge.copy(color = colorScheme.onSurfaceVariant),
        secondaryBold = typography.titleMedium.copy(color = colorScheme.onSurfaceVariant),
        secondarySmall = typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
        secondarySmallBold = typography.titleSmall.copy(color = colorScheme.onSurfaceVariant),
        secondaryExtraSmall = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
    )

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
        LocalBodyTextStyles provides bodyTextStyles,
        LocalTypography provides typography,
        LocalThemeSizes provides themeSizes,
    ) {
        RetainTheme(
            useDarkTheme = useDarkTheme,
            dynamicColor = dynamicColor,
            typography = typography,
            darkColors = darkColors,
            lightColors = lightColors,
            content = content,
        )
    }
}
