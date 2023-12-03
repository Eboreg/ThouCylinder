package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.BugReport
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.ImportExport
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import us.huseli.retaintheme.compose.MainMenuItem
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.retaintheme.compose.SnackbarHosts
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.clone

@Composable
fun ThouCylinderScaffold(
    modifier: Modifier = Modifier,
    activeScreen: String?,
    navController: NavHostController,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onMenuItemClick = { screen: String ->
        when (screen) {
            "search" -> navController.navigate(SearchDestination.route)
            "library" -> navController.navigate(LibraryDestination.route)
            "queue" -> navController.navigate(QueueDestination.route)
            "import" -> navController.navigate(ImportDestination.route)
            "debug" -> navController.navigate(DebugDestination.route)
            "menu" -> scope.launch { drawerState.open() }
            "downloads" -> navController.navigate(DownloadsDestination.route)
            "settings" -> navController.navigate(SettingsDestination.route)
        }
        isCoverExpanded = false
    }
    val baseMenuItems = mutableListOf(
        MainMenuItem("library", Icons.Sharp.LibraryMusic, stringResource(R.string.library)),
        MainMenuItem("queue", Icons.Sharp.QueueMusic, stringResource(R.string.queue)),
        MainMenuItem("search", Icons.Sharp.Search, stringResource(R.string.search)),
        MainMenuItem("import", Icons.Sharp.ImportExport, stringResource(R.string.import_str)),
    )

    val mainMenuItems = baseMenuItems.clone()
    mainMenuItems.add(0, MainMenuItem("menu", Icons.Sharp.Menu, stringResource(R.string.menu)))

    val drawerItems = baseMenuItems.clone()
    drawerItems.add(MainMenuItem("downloads", Icons.Sharp.Download, stringResource(R.string.downloads)))
    drawerItems.add(MainMenuItem("settings", Icons.Sharp.Settings, stringResource(R.string.settings)))
    if (BuildConfig.DEBUG)
        drawerItems.add(MainMenuItem("debug", Icons.Sharp.BugReport, stringResource(R.string.debug)))

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RectangleShape,
                modifier = Modifier.width(IntrinsicSize.Min),
            ) {
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        shape = RectangleShape,
                        label = { item.description?.also { Text(it) } },
                        selected = activeScreen == item.contentScreen,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onMenuItemClick(item.contentScreen)
                        },
                        icon = { Icon(item.icon, null) },
                    )
                }
            }
        }
    ) {
        ResponsiveScaffold(
            portraitMenuModifier = Modifier.height(80.dp),
            activeScreen = activeScreen,
            mainMenuItems = mainMenuItems,
            onMenuItemClick = onMenuItemClick,
            landscapeMenu = { innerPadding ->
                NavigationRail(modifier = Modifier.padding(innerPadding)) {
                    mainMenuItems.forEach { item ->
                        NavigationRailItem(
                            selected = activeScreen == item.contentScreen,
                            onClick = { onMenuItemClick(item.contentScreen) },
                            icon = { Icon(item.icon, null) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHosts() },
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = modifier.fillMaxSize().padding(innerPadding),
                content = content,
            )
        }
    }
}
