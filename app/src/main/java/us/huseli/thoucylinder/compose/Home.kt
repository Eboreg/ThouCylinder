package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.LibraryMusic
import androidx.compose.material.icons.sharp.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.retaintheme.compose.MainMenuItem
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.thoucylinder.AlbumDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.YoutubePlaylistDestination
import us.huseli.thoucylinder.compose.screens.AlbumScreen
import us.huseli.thoucylinder.compose.screens.LibraryScreen
import us.huseli.thoucylinder.compose.screens.YoutubePlaylistScreen
import us.huseli.thoucylinder.compose.screens.YoutubeSearchScreen

@Composable
fun Home(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    var activeScreen by rememberSaveable { mutableStateOf<String?>("search") }

    val mainMenuItems = listOf(
        MainMenuItem("search", Icons.Sharp.Search, stringResource(R.string.search)),
        MainMenuItem("library", Icons.Sharp.LibraryMusic, stringResource(R.string.library)),
    )

    ResponsiveScaffold(
        activeScreen = activeScreen,
        mainMenuItems = mainMenuItems,
        onMenuItemClick = {
            when (it) {
                "search" -> navController.navigate(SearchDestination.route)
                "library" -> navController.navigate(LibraryDestination.route)
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            startDestination = SearchDestination.route,
        ) {
            composable(route = SearchDestination.route) {
                activeScreen = "search"
                YoutubeSearchScreen(
                    onPlaylistClick = { navController.navigate(YoutubePlaylistDestination.route(it)) }
                )
            }

            composable(route = LibraryDestination.route) {
                activeScreen = "library"
                LibraryScreen(
                    onAlbumClick = { navController.navigate(AlbumDestination.route(it)) },
                )
            }

            composable(
                route = YoutubePlaylistDestination.routeTemplate,
                arguments = YoutubePlaylistDestination.arguments,
            ) {
                activeScreen = null
                YoutubePlaylistScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(
                route = AlbumDestination.routeTemplate,
                arguments = AlbumDestination.arguments,
            ) {
                activeScreen = null
                AlbumScreen(
                    onBackClick = { navController.popBackStack() },
                    onYoutubePlaylistClick = { navController.navigate(YoutubePlaylistDestination.route(it)) },
                )
            }
        }
    }
}
