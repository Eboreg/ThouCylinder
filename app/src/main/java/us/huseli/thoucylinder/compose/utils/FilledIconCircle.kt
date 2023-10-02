package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun FilledIconCircle(
    modifier: Modifier = Modifier,
    shape: Shape = IconButtonDefaults.filledShape,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: @Composable BoxScope.() -> Unit,
) = Surface(
    modifier = modifier.semantics { role = Role.Button },
    shape = shape,
    color = color,
    contentColor = contentColorFor(color),
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
