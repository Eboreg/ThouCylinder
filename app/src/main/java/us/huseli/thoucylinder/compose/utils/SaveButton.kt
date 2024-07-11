package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

@Composable
fun SaveButton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(onClick = onClick, shape = shape, modifier = modifier, content = content, enabled = enabled)
}

@Composable
fun SaveButton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SaveButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        content = { Text(text) },
    )
}

@Composable
fun SaveButton(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SaveButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        enabled = enabled,
        text = stringResource(R.string.save),
    )
}
