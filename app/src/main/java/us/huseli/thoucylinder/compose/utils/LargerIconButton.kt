package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import us.huseli.thoucylinder.compose.LocalThemeSizes

@Composable
fun LargerIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    iconTint: Color? = null,
) {
    val sizes = LocalThemeSizes.current

    IconButton(
        onClick = onClick,
        modifier = modifier.size(sizes.largerIconButton),
        enabled = enabled,
        colors = colors,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(sizes.largerIconButtonIcon),
            tint = iconTint ?: LocalContentColor.current,
        )
    }
}
