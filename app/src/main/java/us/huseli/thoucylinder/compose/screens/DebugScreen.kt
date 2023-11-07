package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DebugScreen() {
    Column(modifier = Modifier.padding(10.dp).verticalScroll(state = rememberScrollState())) {
        Row {
            ColorSample(Modifier.weight(0.5f), "background", MaterialTheme.colorScheme.background)
            ColorSample(Modifier.weight(0.5f), "error", MaterialTheme.colorScheme.error)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "errorContainer", MaterialTheme.colorScheme.errorContainer)
            ColorSample(Modifier.weight(0.5f), "inverseOnSurface", MaterialTheme.colorScheme.inverseOnSurface)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "inversePrimary", MaterialTheme.colorScheme.inversePrimary)
            ColorSample(Modifier.weight(0.5f), "inverseSurface", MaterialTheme.colorScheme.inverseSurface)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onBackground", MaterialTheme.colorScheme.onBackground)
            ColorSample(Modifier.weight(0.5f), "onError", MaterialTheme.colorScheme.onError)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onErrorContainer", MaterialTheme.colorScheme.onErrorContainer)
            ColorSample(Modifier.weight(0.5f), "onPrimary", MaterialTheme.colorScheme.onPrimary)
        }
        Row {
            ColorSample(
                Modifier.weight(0.5f),
                "onPrimaryContainer",
                MaterialTheme.colorScheme.onPrimaryContainer
            )
            ColorSample(Modifier.weight(0.5f), "onSecondary", MaterialTheme.colorScheme.onSecondary)
        }
        Row {
            ColorSample(
                Modifier.weight(0.5f),
                "onSecondaryContainer",
                MaterialTheme.colorScheme.onSecondaryContainer
            )
            ColorSample(Modifier.weight(0.5f), "onSurface", MaterialTheme.colorScheme.onSurface)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "onSurfaceVariant", MaterialTheme.colorScheme.onSurfaceVariant)
            ColorSample(Modifier.weight(0.5f), "onTertiary", MaterialTheme.colorScheme.onTertiary)
        }
        Row {
            ColorSample(
                Modifier.weight(0.5f),
                "onTertiaryContainer",
                MaterialTheme.colorScheme.onTertiaryContainer
            )
            ColorSample(Modifier.weight(0.5f), "outline", MaterialTheme.colorScheme.outline)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "outlineVariant", MaterialTheme.colorScheme.outlineVariant)
            ColorSample(Modifier.weight(0.5f), "primary", MaterialTheme.colorScheme.primary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "primaryContainer", MaterialTheme.colorScheme.primaryContainer)
            ColorSample(Modifier.weight(0.5f), "scrim", MaterialTheme.colorScheme.scrim)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "secondary", MaterialTheme.colorScheme.secondary)
            ColorSample(
                Modifier.weight(0.5f),
                "secondaryContainer",
                MaterialTheme.colorScheme.secondaryContainer
            )
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "surface", MaterialTheme.colorScheme.surface)
            ColorSample(Modifier.weight(0.5f), "surfaceTint", MaterialTheme.colorScheme.surfaceTint)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "surfaceVariant", MaterialTheme.colorScheme.surfaceVariant)
            ColorSample(Modifier.weight(0.5f), "tertiary", MaterialTheme.colorScheme.tertiary)
        }
        Row {
            ColorSample(Modifier.weight(0.5f), "tertiaryContainer", MaterialTheme.colorScheme.tertiaryContainer)
        }
    }
}

@Composable
fun ColorSample(modifier: Modifier = Modifier, name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(modifier = Modifier.height(40.dp).width(40.dp).border(1.dp, Color.Black).background(color))
        Text(name, style = MaterialTheme.typography.labelSmall)
    }
}
