package us.huseli.thoucylinder.compose

import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
import androidx.compose.material.icons.sharp.BugReport
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.FileUpload
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Lightbulb
import androidx.compose.material.icons.sharp.Menu
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import us.huseli.retaintheme.compose.MenuItem
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource

enum class MenuItemId(val route: String) {
    SEARCH_YOUTUBE("search-youtube"),
    LIBRARY("library"),
    QUEUE("queue"),
    IMPORT("import"),
    DEBUG("debug"),
    DOWNLOADS("downloads"),
    SETTINGS("settings"),
    MENU("menu"),
    RECOMMENDATIONS("recommendations"),
}

class ThouCylinderMenuItem<MI : Enum<MI>>(
    id: MI,
    icon: @Composable () -> Unit,
    description: String? = null,
    val showInMainMenu: Boolean = true,
    val showInDrawer: Boolean = true,
    val debugOnly: Boolean = false,
) : MenuItem<MI>(id, icon, description) {
    constructor(
        id: MI,
        imageVector: ImageVector,
        description: String? = null,
        showInMainMenu: Boolean = true,
        showInDrawer: Boolean = true,
        debugOnly: Boolean = false,
    ) : this(id, { Icon(imageVector, null) }, description, showInMainMenu, showInDrawer, debugOnly)

    constructor(
        id: MI,
        painter: Painter,
        description: String? = null,
        showInMainMenu: Boolean = true,
        showInDrawer: Boolean = true,
        debugOnly: Boolean = false,
    ) : this(id, { Icon(painter, null) }, description, showInMainMenu, showInDrawer, debugOnly)
}


@Composable
fun getMenuItems(): List<ThouCylinderMenuItem<MenuItemId>> {
    return listOf(
        ThouCylinderMenuItem(MenuItemId.MENU, Icons.Sharp.Menu, stringResource(R.string.menu), showInDrawer = false),
        ThouCylinderMenuItem(MenuItemId.LIBRARY, Icons.Sharp.LibraryMusic, stringResource(R.string.library)),
        ThouCylinderMenuItem(MenuItemId.QUEUE, Icons.AutoMirrored.Sharp.QueueMusic, stringResource(R.string.queue)),
        ThouCylinderMenuItem(
            id = MenuItemId.SEARCH_YOUTUBE,
            painter = painterResource(R.drawable.youtube),
            description = stringResource(R.string.search),
        ),
        ThouCylinderMenuItem(MenuItemId.IMPORT, Icons.Sharp.FileUpload, stringResource(R.string.import_str)),
        ThouCylinderMenuItem(
            id = MenuItemId.DOWNLOADS,
            imageVector = Icons.Sharp.Download,
            description = stringResource(R.string.downloads),
            showInMainMenu = false,
        ),
        ThouCylinderMenuItem(
            id = MenuItemId.RECOMMENDATIONS,
            imageVector = Icons.Sharp.Lightbulb,
            description = stringResource(R.string.recommendations),
            showInMainMenu = false,
        ),
        ThouCylinderMenuItem(
            id = MenuItemId.SETTINGS,
            imageVector = Icons.Sharp.Settings,
            description = stringResource(R.string.settings),
            showInMainMenu = false,
        ),
        ThouCylinderMenuItem(
            MenuItemId.DEBUG,
            Icons.Sharp.BugReport,
            stringResource(R.string.debug),
            debugOnly = true,
            showInMainMenu = false,
        ),
    )
}
