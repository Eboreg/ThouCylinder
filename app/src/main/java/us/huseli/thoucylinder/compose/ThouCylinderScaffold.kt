package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.retaintheme.compose.SnackbarHosts
import us.huseli.thoucylinder.AddDestination
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.RecommendationsDestination
import us.huseli.thoucylinder.SettingsDestination

@Composable
fun ThouCylinderScaffold(
    modifier: Modifier = Modifier,
    activeMenuItemId: MenuItemId?,
    onNavigate: (route: String) -> Unit,
    onRadioClick: () -> Unit,
    content: @Composable (BoxWithConstraintsScope.() -> Unit),
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onMenuItemClick = { menuItem: MenuItemId ->
        if (menuItem != activeMenuItemId) {
            when (menuItem) {
                MenuItemId.SEARCH_YOUTUBE -> onNavigate(AddDestination.route)
                MenuItemId.LIBRARY -> onNavigate(LibraryDestination.route)
                MenuItemId.QUEUE -> onNavigate(QueueDestination.route)
                MenuItemId.IMPORT -> onNavigate(ImportDestination.route)
                MenuItemId.DEBUG -> onNavigate(DebugDestination.route)
                MenuItemId.DOWNLOADS -> onNavigate(DownloadsDestination.route)
                MenuItemId.SETTINGS -> onNavigate(SettingsDestination.route)
                MenuItemId.MENU -> scope.launch { drawerState.open() }
                MenuItemId.RECOMMENDATIONS -> onNavigate(RecommendationsDestination.route)
                MenuItemId.RADIO -> onRadioClick()
            }
        }
    }
    val menuItems = getMenuItems().filter { !it.debugOnly || BuildConfig.DEBUG }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RectangleShape,
                modifier = Modifier.width(IntrinsicSize.Min),
            ) {
                for (item in menuItems.filter { it.showInDrawer }) {
                    item.DrawerItem(
                        activeItemId = activeMenuItemId,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onMenuItemClick(item.id)
                        },
                    )
                }
            }
        }
    ) {
        ResponsiveScaffold(
            portraitMenuModifier = Modifier.height(80.dp),
            activeMenuItemId = activeMenuItemId,
            menuItems = menuItems.filter { it.showInMainMenu },
            onMenuItemClick = onMenuItemClick,
            landscapeMenu = { innerPadding ->
                NavigationRail(modifier = Modifier.padding(innerPadding)) {
                    for (item in menuItems.filter { it.showInMainMenu }) {
                        item.RailItem(activeItemId = activeMenuItemId, onClick = { onMenuItemClick(item.id) })
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
