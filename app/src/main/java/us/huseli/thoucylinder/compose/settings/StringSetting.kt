package us.huseli.thoucylinder.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.compose.FistopyTheme

@Composable
fun StringSetting(
    title: String,
    description: @Composable () -> Unit,
    currentValue: String?,
    onClick: () -> Unit,
) {
    Setting(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = FistopyTheme.typography.titleMedium)
            description()
            currentValue?.also {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = FistopyTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun StringSetting(
    title: String,
    description: String,
    currentValue: String?,
    onClick: () -> Unit,
) {
    StringSetting(
        title = title,
        description = {
            Text(
                text = description,
                style = FistopyTheme.typography.bodyMedium,
            )
        },
        currentValue = currentValue,
        onClick = onClick,
    )
}
