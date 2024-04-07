package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

@Composable
fun CancelButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(modifier = modifier, onClick = onClick, content = content, enabled = enabled)
}

@Composable
fun CancelButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    CancelButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        content = { Text(text) },
    )
}

@Composable
fun CancelButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    CancelButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        text = stringResource(R.string.cancel),
    )
}
