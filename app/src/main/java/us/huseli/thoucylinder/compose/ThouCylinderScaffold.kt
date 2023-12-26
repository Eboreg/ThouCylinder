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
import androidx.compose.material.icons.sharp.FileUpload
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.DrawerValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.huseli.retaintheme.compose.MenuItem
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.retaintheme.compose.SnackbarHosts
import us.huseli.thoucylinder.AddDestination
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.MenuItemId
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.clone

@Composable
fun ThouCylinderScaffold(
    modifier: Modifier = Modifier,
    activeMenuItemId: MenuItemId?,
    onNavigate: (route: String) -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onMenuItemClick = { menuItem: MenuItemId ->
        when (menuItem) {
            MenuItemId.SEARCH_YOUTUBE -> onNavigate(AddDestination.route)
            MenuItemId.LIBRARY -> onNavigate(LibraryDestination.route)
            MenuItemId.QUEUE -> onNavigate(QueueDestination.route)
            MenuItemId.IMPORT -> onNavigate(ImportDestination.route)
            MenuItemId.DEBUG -> onNavigate(DebugDestination.route)
            MenuItemId.DOWNLOADS -> onNavigate(DownloadsDestination.route)
            MenuItemId.SETTINGS -> onNavigate(SettingsDestination.route)
            MenuItemId.MENU -> scope.launch { drawerState.open() }
        }
        isCoverExpanded = false
    }
    val baseMenuItems = mutableListOf(
        MenuItem(MenuItemId.LIBRARY, Icons.Sharp.LibraryMusic, stringResource(R.string.library)),
        MenuItem(MenuItemId.QUEUE, Icons.Sharp.QueueMusic, stringResource(R.string.queue)),
        MenuItem(MenuItemId.SEARCH_YOUTUBE, painterResource(R.drawable.youtube), stringResource(R.string.search)),
        MenuItem(MenuItemId.IMPORT, Icons.Sharp.FileUpload, stringResource(R.string.import_str)),
    )
    val menuItems =
        listOf(MenuItem(MenuItemId.MENU, Icons.Sharp.Menu, stringResource(R.string.menu))).plus(baseMenuItems.clone())
    val drawerItems = baseMenuItems.clone().plus(
        listOf(
            MenuItem(MenuItemId.DOWNLOADS, Icons.Sharp.Download, stringResource(R.string.downloads)),
            MenuItem(MenuItemId.SETTINGS, Icons.Sharp.Settings, stringResource(R.string.settings)),
        )
    ).toMutableList()

    if (BuildConfig.DEBUG)
        drawerItems.add(MenuItem(MenuItemId.DEBUG, Icons.Sharp.BugReport, stringResource(R.string.debug)))

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
                        selected = activeMenuItemId == item.id,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onMenuItemClick(item.id)
                        },
                        icon = item.icon,
                    )
                }
            }
        }
    ) {
        ResponsiveScaffold(
            portraitMenuModifier = Modifier.height(80.dp),
            activeMenuItemId = activeMenuItemId,
            menuItems = menuItems,
            onMenuItemClick = onMenuItemClick,
            landscapeMenu = { innerPadding ->
                NavigationRail(modifier = Modifier.padding(innerPadding)) {
                    menuItems.forEach { item ->
                        NavigationRailItem(
                            selected = activeMenuItemId == item.id,
                            onClick = { onMenuItemClick(item.id) },
                            icon = item.icon,
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
