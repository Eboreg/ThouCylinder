package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.YoutubeAndroidClient
import us.huseli.thoucylinder.YoutubeAndroidEmbeddedClient
import us.huseli.thoucylinder.YoutubeAndroidTestSuiteClient
import us.huseli.thoucylinder.YoutubeAndroidUnpluggedClient
import us.huseli.thoucylinder.YoutubeIOSClient
import us.huseli.thoucylinder.YoutubeWebClient
import us.huseli.thoucylinder.compose.utils.SmallOutlinedButton
import us.huseli.thoucylinder.viewmodels.DebugViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel()) {
    val density = LocalDensity.current
    val context = LocalContext.current

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

    Column(modifier = Modifier.padding(10.dp).verticalScroll(state = rememberScrollState())) {
        Button(
            onClick = { viewModel.clearDatabase() },
            content = { Text("Clear DB") }
        )
        Button(
            onClick = { viewModel.doStartupTasks() },
            content = { Text("Do startup tasks") }
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            clients.forEach { client ->
                SmallOutlinedButton(
                    onClick = { viewModel.getMetadata(client) },
                    text = "getMetadata(${client.clientName})",
                )
                SmallOutlinedButton(
                    onClick = { viewModel.getVideoSearchResult(context, client) },
                    text = "getVideoSearchResult(${client.clientName})",
                )
                SmallOutlinedButton(
                    onClick = { viewModel.searchPlaylistCombos(context, client) },
                    text = "searchPlaylistCombos(${client.clientName})",
                )
                continuationTokens[client.clientName]?.also { token ->
                    SmallOutlinedButton(
                        onClick = { viewModel.getVideoSearchResultContinued(context, client, token) },
                        text = "getVideoSearchResultContinued(${client.clientName})",
                    )
                }
            }
        }

        Text("1 dp = ${with(density) { 1.dp.toPx() }} px")
        Text("1 px = ${with(density) { 1.toDp() }} dp")

        Row {
            ColorSample("background", MaterialTheme.colorScheme.background)
            ColorSample("error", MaterialTheme.colorScheme.error)
        }
        Row {
            ColorSample("errorContainer", MaterialTheme.colorScheme.errorContainer)
            ColorSample("inverseOnSurface", MaterialTheme.colorScheme.inverseOnSurface)
        }
        Row {
            ColorSample("inversePrimary", MaterialTheme.colorScheme.inversePrimary)
            ColorSample("inverseSurface", MaterialTheme.colorScheme.inverseSurface)
        }
        Row {
            ColorSample("onBackground", MaterialTheme.colorScheme.onBackground)
            ColorSample("onError", MaterialTheme.colorScheme.onError)
        }
        Row {
            ColorSample("onErrorContainer", MaterialTheme.colorScheme.onErrorContainer)
            ColorSample("onPrimary", MaterialTheme.colorScheme.onPrimary)
        }
        Row {
            ColorSample("onPrimaryContainer", MaterialTheme.colorScheme.onPrimaryContainer)
            ColorSample("onSecondary", MaterialTheme.colorScheme.onSecondary)
        }
        Row {
            ColorSample("onSecondaryContainer", MaterialTheme.colorScheme.onSecondaryContainer)
            ColorSample("onSurface", MaterialTheme.colorScheme.onSurface)
        }
        Row {
            ColorSample("onSurfaceVariant", MaterialTheme.colorScheme.onSurfaceVariant)
            ColorSample("onTertiary", MaterialTheme.colorScheme.onTertiary)
        }
        Row {
            ColorSample("onTertiaryContainer", MaterialTheme.colorScheme.onTertiaryContainer)
            ColorSample("outline", MaterialTheme.colorScheme.outline)
        }
        Row {
            ColorSample("outlineVariant", MaterialTheme.colorScheme.outlineVariant)
            ColorSample("primary", MaterialTheme.colorScheme.primary)
        }
        Row {
            ColorSample("primaryContainer", MaterialTheme.colorScheme.primaryContainer)
            ColorSample("scrim", MaterialTheme.colorScheme.scrim)
        }
        Row {
            ColorSample("secondary", MaterialTheme.colorScheme.secondary)
            ColorSample("secondaryContainer", MaterialTheme.colorScheme.secondaryContainer)
        }
        Row {
            ColorSample("surface", MaterialTheme.colorScheme.surface)
            ColorSample("surfaceTint", MaterialTheme.colorScheme.surfaceTint)
        }
        Row {
            ColorSample("surfaceVariant", MaterialTheme.colorScheme.surfaceVariant)
            ColorSample("tertiary", MaterialTheme.colorScheme.tertiary)
        }
        Row {
            ColorSample("tertiaryContainer", MaterialTheme.colorScheme.tertiaryContainer)
            ColorSample("Blue", LocalBasicColors.current.Blue)
        }
        Row {
            ColorSample("Brown", LocalBasicColors.current.Brown)
            ColorSample("Gray", LocalBasicColors.current.Gray)
        }
        Row {
            ColorSample("Green", LocalBasicColors.current.Green)
            ColorSample("Orange", LocalBasicColors.current.Orange)
        }
        Row {
            ColorSample("Cerulean", LocalBasicColors.current.Cerulean)
            ColorSample("Pink", LocalBasicColors.current.Pink)
        }
        Row {
            ColorSample("Purple", LocalBasicColors.current.Purple)
            ColorSample("Red", LocalBasicColors.current.Red)
        }
        Row {
            ColorSample("Teal", LocalBasicColors.current.Teal)
            ColorSample("Yellow", LocalBasicColors.current.Yellow)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("displayLarge", style = MaterialTheme.typography.displayLarge)
            Text("displayMedium", style = MaterialTheme.typography.displayMedium)
            Text("displaySmall", style = MaterialTheme.typography.displaySmall)
            Text("headlineLarge", style = MaterialTheme.typography.headlineLarge)
            Text("headlineMedium", style = MaterialTheme.typography.headlineMedium)
            Text("headlineSmall", style = MaterialTheme.typography.headlineSmall)
            Text("MaterialTheme.typography.titleLarge", style = MaterialTheme.typography.titleLarge)
            Text("MaterialTheme.typography.titleMedium", style = MaterialTheme.typography.titleMedium)
            Text("MaterialTheme.typography.titleSmall", style = MaterialTheme.typography.titleSmall)
            Text("MaterialTheme.typography.bodyLarge", style = MaterialTheme.typography.bodyLarge)
            Text("MaterialTheme.typography.bodyMedium", style = MaterialTheme.typography.bodyMedium)
            Text("MaterialTheme.typography.bodySmall", style = MaterialTheme.typography.bodySmall)
            Text("MaterialTheme.typography.labelLarge", style = MaterialTheme.typography.labelLarge)
            Text("MaterialTheme.typography.labelMedium", style = MaterialTheme.typography.labelMedium)
            Text("MaterialTheme.typography.labelSmall", style = MaterialTheme.typography.labelSmall)
            Text(
                "ThouCylinderTheme...listNormalHeader",
                style = ThouCylinderTheme.typographyExtended.listNormalHeader,
            )
            Text(
                "ThouCylinderTheme...listNormalTitle",
                style = ThouCylinderTheme.typographyExtended.listNormalTitle,
            )
            Text(
                "ThouCylinderTheme...listNormalTitleSecondary",
                style = ThouCylinderTheme.typographyExtended.listNormalTitleSecondary,
            )
            Text(
                "ThouCylinderTheme...listSmallHeader",
                style = ThouCylinderTheme.typographyExtended.listSmallHeader,
            )
            Text(
                "ThouCylinderTheme...listSmallTitle",
                style = ThouCylinderTheme.typographyExtended.listSmallTitle,
            )
            Text(
                "ThouCylinderTheme...listSmallTitleSecondary",
                style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
            )
            Text(text = "Default text")
        }
    }
}

@Composable
fun RowScope.ColorSample(name: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.weight(0.5f),
    ) {
        Box(modifier = Modifier.height(40.dp).width(40.dp).border(1.dp, Color.Black).background(color))
        Text(name, style = MaterialTheme.typography.labelSmall)
    }
}
