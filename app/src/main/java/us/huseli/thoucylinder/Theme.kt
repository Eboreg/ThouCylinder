package us.huseli.thoucylinder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import us.huseli.retaintheme.ui.theme.DarkColors
import us.huseli.retaintheme.ui.theme.LightColors
import us.huseli.retaintheme.ui.theme.RetainTheme
import us.huseli.retaintheme.ui.theme.getColorScheme

@Immutable
data class TypographyExtended(
    val listNormalHeader: TextStyle = TextStyle.Default,
    val listNormalTitle: TextStyle = TextStyle.Default,
    val listNormalTitleSecondary: TextStyle = TextStyle.Default,
    val listNormalSubtitle: TextStyle = TextStyle.Default,
    val listNormalSubtitleSecondary: TextStyle = TextStyle.Default,
    val listSmallHeader: TextStyle = TextStyle.Default,
    val listSmallTitle: TextStyle = TextStyle.Default,
    val listSmallTitleSecondary: TextStyle = TextStyle.Default,
)

val LocalTypographyExtended = staticCompositionLocalOf { TypographyExtended() }

object ThouCylinderTheme {
    val typographyExtended: TypographyExtended
        @Composable
        @ReadOnlyComposable
        get() = LocalTypographyExtended.current
}

@Composable
fun ThouCylinderTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkColors = DarkColors.copy(background = DarkColors.surface)
    val lightColors = LightColors.copy(background = LightColors.surface)
    val colorScheme = getColorScheme(
        lightColors = lightColors,
        darkColors = darkColors,
        dynamicColor = dynamicColor
    )
    val typography = Typography()

    val typographyExtended = TypographyExtended(
        listNormalHeader = TextStyle.Default.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
        ),
        listNormalTitle = TextStyle.Default.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
        ),
        listNormalTitleSecondary = TextStyle.Default.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
            color = colorScheme.onSurfaceVariant,
        ),
        listNormalSubtitle = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
        ),
        listNormalSubtitleSecondary = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            color = colorScheme.onSurfaceVariant,
        ),
        listSmallHeader = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        ),
        listSmallTitle = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
        ),
        listSmallTitleSecondary = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            color = colorScheme.onSurfaceVariant,
        ),
    )

    CompositionLocalProvider(LocalTypographyExtended provides typographyExtended) {
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
