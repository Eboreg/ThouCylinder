package us.huseli.thoucylinder.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
import androidx.compose.material.icons.sharp.BugReport
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Downloading
import androidx.compose.material.icons.sharp.FileUpload
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

enum class MenuItemId(val route: String) {
    SEARCH("search"),
    LIBRARY("library"),
    QUEUE("queue"),
    IMPORT("import"),
    DEBUG("debug"),
    DOWNLOADS("downloads"),
    SETTINGS("settings"),
    MENU("menu"),
    RADIO("radio"),
    EXPORT("export"),
}

data class FistopyMenuItem(
    val id: MenuItemId,
    val icon: ImageVector,
    @StringRes val stringId: Int,
    val showInMainMenu: Boolean = true,
    val showInDrawer: Boolean = true,
    val debugOnly: Boolean = false,
)

val menuItems = listOf(
    FistopyMenuItem(
        id = MenuItemId.MENU,
        icon = Icons.Sharp.Menu,
        stringId = R.string.menu,
        showInDrawer = false,
    ),
    FistopyMenuItem(
        id = MenuItemId.LIBRARY,
        icon = Icons.Sharp.LibraryMusic,
        stringId = R.string.library,
    ),
    FistopyMenuItem(
        id = MenuItemId.QUEUE,
        icon = Icons.AutoMirrored.Sharp.QueueMusic,
        stringId = R.string.queue,
    ),
    FistopyMenuItem(
        id = MenuItemId.SEARCH,
        icon = Icons.Sharp.Search,
        stringId = R.string.search,
    ),
    FistopyMenuItem(
        id = MenuItemId.IMPORT,
        icon = Icons.Sharp.FileUpload,
        stringId = R.string.import_str,
    ),
    FistopyMenuItem(
        id = MenuItemId.EXPORT,
        icon = Icons.Sharp.Download,
        stringId = R.string.export,
        showInMainMenu = false,
    ),
    FistopyMenuItem(
        id = MenuItemId.DOWNLOADS,
        icon = Icons.Sharp.Downloading,
        stringId = R.string.downloads,
        showInMainMenu = false,
    ),
    FistopyMenuItem(
        id = MenuItemId.SETTINGS,
        icon = Icons.Sharp.Settings,
        stringId = R.string.settings,
        showInMainMenu = false,
    ),
    FistopyMenuItem(
        id = MenuItemId.DEBUG,
        icon = Icons.Sharp.BugReport,
        stringId = R.string.debug,
        debugOnly = true,
        showInMainMenu = false,
    ),
    FistopyMenuItem(
        id = MenuItemId.RADIO,
        icon = Icons.Sharp.Radio,
        stringId = R.string.radio,
        showInMainMenu = false,
    ),
)

@Composable
fun rememberDrawerMenuItems() = remember {
    menuItems.filter { it.showInDrawer }.filter { !it.debugOnly || BuildConfig.DEBUG }
}

@Composable
fun rememberMainMenuItems() = remember {
    menuItems.filter { it.showInMainMenu }.filter { !it.debugOnly || BuildConfig.DEBUG }
}

@Composable
fun DrawerMenu(activeMenuItemId: MenuItemId?, onClick: (MenuItemId) -> Unit, modifier: Modifier = Modifier) {
    val items = rememberDrawerMenuItems()

    ModalDrawerSheet(
        drawerShape = RectangleShape,
        modifier = modifier.width(IntrinsicSize.Min),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    ) {
        Image(
            painter = painterResource(R.mipmap.logo_wide),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            for (item in items) {
                NavigationDrawerItem(
                    shape = RectangleShape,
                    label = { Text(stringResource(item.stringId)) },
                    selected = activeMenuItemId == item.id,
                    onClick = { if (item.id != activeMenuItemId) onClick(item.id) },
                    icon = { Icon(item.icon, null) },
                )
            }
        }
    }
}

@Composable
fun LandscapeMenu(activeMenuItemId: MenuItemId?, onClick: (MenuItemId) -> Unit, modifier: Modifier = Modifier) {
    val items = rememberMainMenuItems()

    NavigationRail(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        for (item in items) {
            NavigationRailItem(
                selected = activeMenuItemId == item.id,
                onClick = { if (item.id != activeMenuItemId) onClick(item.id) },
                icon = { Icon(item.icon, null) },
            )
        }
    }
}

@Composable
fun PortraitMenu(
    activeMenuItemId: MenuItemId?,
    onClick: (MenuItemId) -> Unit,
    importProgress: () -> Double?,
    modifier: Modifier = Modifier
) {
    val items = rememberMainMenuItems()

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.height(80.dp),
    ) {
        for (item in items) {
            NavigationBarItem(
                selected = activeMenuItemId == item.id,
                onClick = { if (item.id != activeMenuItemId) onClick(item.id) },
                icon = { Icon(item.icon, null) },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val progress = importProgress()

                        Text(stringResource(item.stringId))
                        if (item.id == MenuItemId.IMPORT && progress != null) {
                            LinearProgressIndicator(
                                progress = { progress.toFloat() },
                                modifier = Modifier.height(2.dp).padding(horizontal = 5.dp),
                                drawStopIndicator = {},
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                },
            )
        }
    }
}
