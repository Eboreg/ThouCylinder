package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.YoutubeAndroidClient
import us.huseli.thoucylinder.YoutubeAndroidEmbeddedClient
import us.huseli.thoucylinder.YoutubeAndroidTestSuiteClient
import us.huseli.thoucylinder.YoutubeAndroidUnpluggedClient
import us.huseli.thoucylinder.YoutubeIOSClient
import us.huseli.thoucylinder.YoutubeWebClient
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.viewmodels.DebugViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val density = LocalDensity.current

    val region by viewModel.region.collectAsStateWithLifecycle()
    val continuationTokens by viewModel.continuationTokens.collectAsStateWithLifecycle()

    val clients = remember(region) {
        mutableStateListOf(
            YoutubeAndroidTestSuiteClient(region),
            YoutubeAndroidClient(region),
            YoutubeAndroidEmbeddedClient(region),
            YoutubeAndroidUnpluggedClient(region),
            YoutubeIOSClient(region),
            YoutubeWebClient(region),
        )
    }

    Column(
        modifier = Modifier.padding(10.dp).verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { viewModel.clearDatabase() },
                content = { Text("Clear DB") }
            )
            Button(
                onClick = { viewModel.doStartupTasks() },
                content = { Text("Do startup tasks") }
            )
            Button(
                onClick = { SnackbarEngine.addInfo("Umpo bumpo español") },
                content = { Text("Show snackbar") },
            )
        }

        Text("1 dp = ${with(density) { 1.dp.toPx() }} px")
        Text("1 px = ${with(density) { 1.toDp() }} dp")

        Column {
            val colorSamples = mapOf(
                "background" to MaterialTheme.colorScheme.background,
                "error" to MaterialTheme.colorScheme.error,
                "errorContainer" to MaterialTheme.colorScheme.errorContainer,
                "inverseOnSurface" to MaterialTheme.colorScheme.inverseOnSurface,
                "inversePrimary" to MaterialTheme.colorScheme.inversePrimary,
                "inverseSurface" to MaterialTheme.colorScheme.inverseSurface,
                "onBackground" to MaterialTheme.colorScheme.onBackground,
                "onError" to MaterialTheme.colorScheme.onError,
                "onErrorContainer" to MaterialTheme.colorScheme.onErrorContainer,
                "onPrimary" to MaterialTheme.colorScheme.onPrimary,
                "onPrimaryContainer" to MaterialTheme.colorScheme.onPrimaryContainer,
                "onSecondary" to MaterialTheme.colorScheme.onSecondary,
                "onSecondaryContainer" to MaterialTheme.colorScheme.onSecondaryContainer,
                "onSurface" to MaterialTheme.colorScheme.onSurface,
                "onSurfaceVariant" to MaterialTheme.colorScheme.onSurfaceVariant,
                "onTertiary" to MaterialTheme.colorScheme.onTertiary,
                "onTertiaryContainer" to MaterialTheme.colorScheme.onTertiaryContainer,
                "outline" to MaterialTheme.colorScheme.outline,
                "outlineVariant" to MaterialTheme.colorScheme.outlineVariant,
                "primary" to MaterialTheme.colorScheme.primary,
                "primaryContainer" to MaterialTheme.colorScheme.primaryContainer,
                "scrim" to MaterialTheme.colorScheme.scrim,
                "secondary" to MaterialTheme.colorScheme.secondary,
                "secondaryContainer" to MaterialTheme.colorScheme.secondaryContainer,
                "surface" to MaterialTheme.colorScheme.surface,
                "surfaceBright" to MaterialTheme.colorScheme.surfaceBright,
                "surfaceContainer" to MaterialTheme.colorScheme.surfaceContainer,
                "surfaceContainerHigh" to MaterialTheme.colorScheme.surfaceContainerHigh,
                "surfaceContainerHighest" to MaterialTheme.colorScheme.surfaceContainerHighest,
                "surfaceContainerLow" to MaterialTheme.colorScheme.surfaceContainerLow,
                "surfaceContainerLowest" to MaterialTheme.colorScheme.surfaceContainerLowest,
                "surfaceDim" to MaterialTheme.colorScheme.surfaceDim,
                "surfaceTint" to MaterialTheme.colorScheme.surfaceTint,
                "surfaceVariant" to MaterialTheme.colorScheme.surfaceVariant,
                "tertiary" to MaterialTheme.colorScheme.tertiary,
                "tertiaryContainer" to MaterialTheme.colorScheme.tertiaryContainer,
                "Blue" to LocalBasicColors.current.Blue,
                "Brown" to LocalBasicColors.current.Brown,
                "Gray" to LocalBasicColors.current.Gray,
                "Green" to LocalBasicColors.current.Green,
                "Orange" to LocalBasicColors.current.Orange,
                "Cerulean" to LocalBasicColors.current.Cerulean,
                "Pink" to LocalBasicColors.current.Pink,
                "Purple" to LocalBasicColors.current.Purple,
                "Red" to LocalBasicColors.current.Red,
                "Teal" to LocalBasicColors.current.Teal,
                "Yellow" to LocalBasicColors.current.Yellow,
            )

            ColorSamples(samples = colorSamples)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            var sampleText by rememberSaveable { mutableStateOf("Hora") }

            OutlinedTextField(
                value = sampleText,
                onValueChange = { sampleText = it },
                placeholder = { Text("Sample text") },
                singleLine = true,
                supportingText = { Text("Sample text") },
            )

            Text("FistopyTheme.typography:", style = FistopyTheme.typography.titleMedium)

            FontSample(name = "displayLarge", style = FistopyTheme.typography.displayLarge, sample = sampleText)
            FontSample(name = "displayMedium", style = FistopyTheme.typography.displayMedium, sample = sampleText)
            FontSample(name = "displaySmall", style = FistopyTheme.typography.displaySmall, sample = sampleText)
            FontSample(name = "headlineLarge", style = FistopyTheme.typography.headlineLarge, sample = sampleText)
            FontSample(
                name = "headlineMedium",
                style = FistopyTheme.typography.headlineMedium,
                sample = sampleText,
            )
            FontSample(name = "headlineSmall", style = FistopyTheme.typography.headlineSmall, sample = sampleText)
            FontSample(name = "titleLarge", style = FistopyTheme.typography.titleLarge, sample = sampleText)
            FontSample(name = "titleMedium", style = FistopyTheme.typography.titleMedium, sample = sampleText)
            FontSample(name = "titleSmall", style = FistopyTheme.typography.titleSmall, sample = sampleText)
            FontSample(name = "bodyLarge", style = FistopyTheme.typography.bodyLarge, sample = sampleText)
            FontSample(name = "bodyMedium", style = FistopyTheme.typography.bodyMedium, sample = sampleText)
            FontSample(name = "bodySmall", style = FistopyTheme.typography.bodySmall, sample = sampleText)
            FontSample(name = "labelLarge", style = FistopyTheme.typography.labelLarge, sample = sampleText)
            FontSample(name = "labelMedium", style = FistopyTheme.typography.labelMedium, sample = sampleText)
            FontSample(name = "labelSmall", style = FistopyTheme.typography.labelSmall, sample = sampleText)

            Text("FistopyTheme.bodyStyles:", style = FistopyTheme.typography.titleMedium)

            FontSample(name = "primary", style = FistopyTheme.bodyStyles.primary, sample = sampleText)
            FontSample(name = "primaryBold", style = FistopyTheme.bodyStyles.primaryBold, sample = sampleText)
            FontSample(name = "primarySmall", style = FistopyTheme.bodyStyles.primarySmall, sample = sampleText)
            FontSample(
                name = "primarySmallBold",
                style = FistopyTheme.bodyStyles.primarySmallBold,
                sample = sampleText,
            )
            FontSample(
                name = "primaryExtraSmall",
                style = FistopyTheme.bodyStyles.primaryExtraSmall,
                sample = sampleText,
            )
            FontSample(name = "secondary", style = FistopyTheme.bodyStyles.secondary, sample = sampleText)
            FontSample(name = "secondaryBold", style = FistopyTheme.bodyStyles.secondaryBold, sample = sampleText)
            FontSample(
                name = "secondarySmall",
                style = FistopyTheme.bodyStyles.secondarySmall,
                sample = sampleText,
            )
            FontSample(
                name = "secondarySmallBold",
                style = FistopyTheme.bodyStyles.secondarySmallBold,
                sample = sampleText,
            )
            FontSample(
                name = "secondaryExtraSmall",
                style = FistopyTheme.bodyStyles.secondaryExtraSmall,
                sample = sampleText,
            )

            FontSample(style = null, name = "Default text", sample = sampleText)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            clients.forEach { client ->
                SmallOutlinedButton(
                    onClick = { viewModel.getMetadata(client) },
                    text = "getMetadata(${client.clientName})",
                )
                SmallOutlinedButton(
                    onClick = { viewModel.getVideoSearchResult(client) },
                    text = "getVideoSearchResult(${client.clientName})",
                )
                SmallOutlinedButton(
                    onClick = { viewModel.searchPlaylistCombos(client) },
                    text = "searchPlaylistCombos(${client.clientName})",
                )
                continuationTokens[client.clientName]?.also { token ->
                    SmallOutlinedButton(
                        onClick = { viewModel.getVideoSearchResultContinued(client, token) },
                        text = "getVideoSearchResultContinued(${client.clientName})",
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSamples(samples: Map<String, Color>) {
    for (chunk in samples.toList().chunked(2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (sample in chunk) {
                ColorSample(name = sample.first, color = sample.second)
            }
        }
    }
}

@Composable
fun RowScope.ColorSample(name: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.weight(0.5f),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(modifier = Modifier.size(40.dp).border(1.dp, Color.Black).background(color))
        Text(name, style = FistopyTheme.typography.labelSmall)
    }
}

@Composable
fun FontSample(style: TextStyle?, name: String, sample: String) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        ) {
            Text(name)
            if (style != null) Text(sample, style = style)
            else Text(sample)
        }

        if (isExpanded && style != null) {
            Text(
                "fontSize=${style.fontSize}, fontWeight=${style.fontWeight}, letterSpacing=${style.letterSpacing}, " +
                    "lineHeight=${style.lineHeight}",
                style = FistopyTheme.typography.bodySmall,
            )
        }
    }
}
