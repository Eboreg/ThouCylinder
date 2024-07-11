package us.huseli.thoucylinder.compose.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import us.huseli.thoucylinder.compose.FistopyTheme

@Composable
fun OutlinedTextFieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.outline,
        style = FistopyTheme.typography.bodySmall,
    )
}