package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retaintheme.compose.MainMenuItem
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.thoucylinder.AlbumDestination
import us.huseli.thoucylinder.ArtistDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.PlaylistDestination
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.screens.AlbumScreen
import us.huseli.thoucylinder.compose.screens.ArtistScreen
import us.huseli.thoucylinder.compose.screens.LibraryScreen
import us.huseli.thoucylinder.compose.screens.PlaylistScreen
import us.huseli.thoucylinder.compose.screens.SearchScreen
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.SearchViewModel
import java.util.UUID

@Composable
fun App(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val playerCurrentPositionMs by viewModel.playerCurrentPositionMs.collectAsStateWithLifecycle()
    val playerPlaybackState by viewModel.playerPlaybackState.collectAsStateWithLifecycle()
    val playerCurrentTrack by viewModel.playerCurrentTrack.collectAsStateWithLifecycle()
    var activeScreen by rememberSaveable { mutableStateOf<String?>("search") }
    val currentTrackImageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    var addToPlaylistSelection by rememberSaveable { mutableStateOf<Selection?>(null) }
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(Unit) {
        viewModel.deleteOrphanTracksAndAlbums()
        viewModel.importNewMediaStoreAlbums()
    }

    LaunchedEffect(playerCurrentTrack) {
        playerCurrentTrack?.image?.also { currentTrackImageBitmap.value = viewModel.getImageBitmap(it) }
    }

    val mainMenuItems = listOf(
        MainMenuItem("search", Icons.Sharp.Search),
        MainMenuItem("library", Icons.Sharp.LibraryMusic),
    )

    val onMenuItemClick = { screen: String ->
        when (screen) {
            "search" -> navController.navigate(SearchDestination.route)
            "library" -> navController.navigate(LibraryDestination.route)
        }
    }

    val onAlbumClick = { albumId: UUID ->
        navController.navigate(AlbumDestination.route(albumId))
    }

    val onArtistClick = { artist: String ->
        navController.navigate(ArtistDestination.route(artist))
    }

    val onPlaylistClick = { playlistId: UUID ->
        navController.navigate(PlaylistDestination.route(playlistId))
    }

    val onAddToPlaylistClick = { selection: Selection ->
        addToPlaylistSelection = selection
    }

    val onBackClick: () -> Unit = {
        navController.popBackStack()
    }

    addToPlaylistSelection?.also { selection ->
        AddToPlaylistDialog(
            playlists = playlists,
            onSelect = { playlist ->
                viewModel.addSelectionToPlaylist(selection, playlist)
                addToPlaylistSelection = null
            },
            onCancel = { addToPlaylistSelection = null }
        )
    }

    ResponsiveScaffold(
        portraitMenuModifier = Modifier.height(50.dp),
        activeScreen = activeScreen,
        mainMenuItems = mainMenuItems,
        onMenuItemClick = onMenuItemClick,
        bottomBar = {
            playerCurrentTrack?.also { track ->
                BottomBar(
                    currentTrack = track,
                    playbackState = playerPlaybackState,
                    currentPositionMs = playerCurrentPositionMs,
                    imageBitmap = currentTrackImageBitmap.value,
                    onPlayOrPauseClick = { playerCurrentTrack?.also { viewModel.playOrPause(it) } },
                    onNextClick = { },
                )
            }
        },
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
        }
    ) { innerPadding ->
        NavHost(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            startDestination = SearchDestination.route,
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
    }
}
