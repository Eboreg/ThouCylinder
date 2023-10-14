package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.QueueMusic
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retaintheme.compose.MainMenuItem
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.retaintheme.compose.SnackbarHosts
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AlbumDestination
import us.huseli.thoucylinder.ArtistDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.PlaylistDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.screens.AlbumScreen
import us.huseli.thoucylinder.compose.screens.ArtistScreen
import us.huseli.thoucylinder.compose.screens.LibraryScreen
import us.huseli.thoucylinder.compose.screens.PlaylistScreen
import us.huseli.thoucylinder.compose.screens.QueueScreen
import us.huseli.thoucylinder.compose.screens.SearchScreen
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import us.huseli.thoucylinder.viewmodels.SearchViewModel
import java.util.UUID

@Composable
fun App(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
) {
    var activeScreen by rememberSaveable { mutableStateOf<String?>("library") }
    var addToPlaylistSelection by rememberSaveable { mutableStateOf<Selection?>(null) }
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    val currentPojo by queueViewModel.playerCurrentPojo.collectAsStateWithLifecycle()
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    var isCreatePlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.importNewMediaStoreAlbums()
        viewModel.deleteOrphanTracksAndAlbums()
    }

    val mainMenuItems = listOf(
        MainMenuItem("search", Icons.Sharp.Search, stringResource(R.string.search)),
        MainMenuItem("library", Icons.Sharp.LibraryMusic, stringResource(R.string.library)),
        MainMenuItem("queue", Icons.AutoMirrored.Sharp.QueueMusic, stringResource(R.string.queue)),
    )

    val onMenuItemClick = { screen: String ->
        when (screen) {
            "search" -> navController.navigate(SearchDestination.route)
            "library" -> navController.navigate(LibraryDestination.route)
            "queue" -> navController.navigate(QueueDestination.route)
        }
        isCoverExpanded = false
    }

    val onAlbumClick = { albumId: UUID ->
        navController.navigate(AlbumDestination.route(albumId))
        isCoverExpanded = false
    }

    val onArtistClick = { artist: String ->
        navController.navigate(ArtistDestination.route(artist))
        isCoverExpanded = false
    }

    val onPlaylistClick = { playlistId: UUID ->
        navController.navigate(PlaylistDestination.route(playlistId))
        isCoverExpanded = false
    }

    val onAddToPlaylistClick = { selection: Selection ->
        addToPlaylistSelection = selection
        isAddToPlaylistDialogOpen = true
    }

    val onBackClick: () -> Unit = {
        navController.popBackStack()
    }

    val displayAddedToPlaylistMessage: (UUID) -> Unit = { playlistId ->
        SnackbarEngine.addInfo(
            message = context.getString(R.string.selection_was_added_to_playlist),
            actionLabel = context.getString(R.string.go_to_playlist),
            onActionPerformed = { onPlaylistClick(playlistId) },
        )
    }

    if (isAddToPlaylistDialogOpen) {
        addToPlaylistSelection?.also { selection ->
            AddToPlaylistDialog(
                playlists = playlists,
                onSelect = { playlist ->
                    viewModel.addSelectionToPlaylist(selection, playlist)
                    isAddToPlaylistDialogOpen = false
                    addToPlaylistSelection = null
                    displayAddedToPlaylistMessage(playlist.playlistId)
                },
                onCancel = {
                    isAddToPlaylistDialogOpen = false
                    addToPlaylistSelection = null
                },
                onCreateNewClick = {
                    isCreatePlaylistDialogOpen = true
                    isAddToPlaylistDialogOpen = false
                }
            )
        }
    }

    if (isCreatePlaylistDialogOpen) {
        CreatePlaylistDialog(
            onSave = { name ->
                val pojo = PlaylistPojo(name = name)

                isCreatePlaylistDialogOpen = false
                viewModel.createPlaylist(pojo, addToPlaylistSelection)

                if (addToPlaylistSelection != null) {
                    displayAddedToPlaylistMessage(pojo.playlistId)
                    addToPlaylistSelection = null
                } else onPlaylistClick(pojo.playlistId)
            },
            onCancel = { isCreatePlaylistDialogOpen = false },
        )
    }

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
            modifier = modifier.fillMaxSize().padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = LibraryDestination.route,
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = if (currentPojo != null) 80.dp else 0.dp)
            ) {
                composable(route = SearchDestination.route) {
                    activeScreen = "search"
                    SearchScreen(
                        viewModel = searchViewModel,
                        onAlbumClick = onAlbumClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }

                composable(route = LibraryDestination.route) {
                    activeScreen = "library"
                    LibraryScreen(
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onCreatePlaylistClick = { isCreatePlaylistDialogOpen = true },
                    )
                }

                composable(route = QueueDestination.route) {
                    activeScreen = "queue"
                    QueueScreen(
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                    )
                }

                composable(
                    route = AlbumDestination.routeTemplate,
                    arguments = AlbumDestination.arguments,
                ) {
                    activeScreen = null
                    AlbumScreen(
                        onBackClick = onBackClick,
                        onArtistClick = onArtistClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }

                composable(
                    route = ArtistDestination.routeTemplate,
                    arguments = ArtistDestination.arguments,
                ) {
                    activeScreen = null
                    ArtistScreen(
                        onBackClick = onBackClick,
                        onAlbumClick = onAlbumClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }

                composable(
                    route = PlaylistDestination.routeTemplate,
                    arguments = PlaylistDestination.arguments,
                ) {
                    activeScreen = null
                    PlaylistScreen(
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onBackClick = onBackClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                    )
                }
            }

            if (currentPojo != null) {
                ModalCover(
                    pojo = currentPojo,
                    viewModel = queueViewModel,
                    isExpanded = isCoverExpanded,
                    onExpand = { isCoverExpanded = true },
                    onCollapse = { isCoverExpanded = false },
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                )
            }
        }
    }
}
