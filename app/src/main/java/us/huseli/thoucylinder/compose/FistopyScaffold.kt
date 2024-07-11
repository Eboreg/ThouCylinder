package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.retaintheme.compose.SnackbarHosts
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.viewmodels.AppViewModel

@Composable
fun FistopyScaffold(
    modifier: Modifier = Modifier,
    activeMenuItemId: MenuItemId?,
    onNavigate: (route: String) -> Unit,
    snackbarModifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
    content: @Composable (BoxScope.() -> Unit),
) {
    val albumImportProgress by viewModel.albumImportProgress.collectAsStateWithLifecycle()
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onMenuItemClick: (MenuItemId) -> Unit = remember {
        { id ->
            when (id) {
                MenuItemId.SEARCH -> onNavigate(SearchDestination.route)
                MenuItemId.LIBRARY -> onNavigate(LibraryDestination.route)
                MenuItemId.QUEUE -> onNavigate(QueueDestination.route)
                MenuItemId.IMPORT -> onNavigate(ImportDestination.route)
                MenuItemId.DEBUG -> onNavigate(DebugDestination.route)
                MenuItemId.DOWNLOADS -> onNavigate(DownloadsDestination.route)
                MenuItemId.SETTINGS -> onNavigate(SettingsDestination.route)
                MenuItemId.MENU -> scope.launch { drawerState.open() }
                MenuItemId.RADIO -> dialogCallbacks.onRadioClick()
                MenuItemId.EXPORT -> dialogCallbacks.onExportAllTracksClick()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu(
                activeMenuItemId = activeMenuItemId,
                onClick = {
                    scope.launch { drawerState.close() }
                    onMenuItemClick(it)
                }
            )
        }
    ) {
        ResponsiveScaffold(
            portraitMenuModifier = Modifier.height(80.dp),
            activeMenuItemId = activeMenuItemId,
            menuItems = emptyList(),
            onMenuItemClick = onMenuItemClick,
            landscapeMenu = { innerPadding ->
                LandscapeMenu(
                    activeMenuItemId = activeMenuItemId,
                    onClick = onMenuItemClick,
                    modifier = Modifier.padding(innerPadding),
                )
            },
            portraitMenu = {
                PortraitMenu(
                    activeMenuItemId = activeMenuItemId,
                    onClick = onMenuItemClick,
                    importProgress = { albumImportProgress },
                )
            },
            snackbarHost = { SnackbarHosts(modifier = snackbarModifier) },
        ) { innerPadding ->
            Box(
                content = content,
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .onSizeChanged { viewModel.setContentSize(it.toSize()) }
            )
        }
    }
}
