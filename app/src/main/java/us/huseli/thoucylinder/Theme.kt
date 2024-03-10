package us.huseli.thoucylinder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
    val listSmallHeader: TextStyle = TextStyle.Default,
    val listSmallTitle: TextStyle = TextStyle.Default,
    val listSmallTitleSecondary: TextStyle = TextStyle.Default,
    val listExtraSmallTitle: TextStyle = TextStyle.Default,
    val listExtraSmallTitleSecondary: TextStyle = TextStyle.Default,
)

val LocalTypographyExtended = staticCompositionLocalOf { TypographyExtended() }

object ThouCylinderTheme {
    val typographyExtended: TypographyExtended
        @Composable
        @ReadOnlyComposable
        get() = LocalTypographyExtended.current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThouCylinderTheme(
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

    val typographyExtended = TypographyExtended(
        listNormalHeader = TextStyle.Default.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
        ),
        listNormalTitle = TextStyle.Default.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
        ),
        listNormalTitleSecondary = TextStyle.Default.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
            color = colorScheme.onSurfaceVariant,
        ),
        listSmallHeader = TextStyle.Default.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        ),
        listSmallTitle = TextStyle.Default.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
        ),
        listSmallTitleSecondary = TextStyle.Default.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            color = colorScheme.onSurfaceVariant,
        ),
        listExtraSmallTitle = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
        ),
        listExtraSmallTitleSecondary = TextStyle.Default.copy(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
            color = colorScheme.onSurfaceVariant,
        ),
    )

    CompositionLocalProvider(
        LocalTypographyExtended provides typographyExtended,
        LocalMinimumInteractiveComponentEnforcement provides false,
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
