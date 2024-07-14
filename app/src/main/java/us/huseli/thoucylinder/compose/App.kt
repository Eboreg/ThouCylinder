package us.huseli.thoucylinder.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import us.huseli.thoucylinder.AlbumDestination
import us.huseli.thoucylinder.ArtistDestination
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.Constants.NAV_ARG_ALBUM
import us.huseli.thoucylinder.Constants.NAV_ARG_ARTIST
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.PlaylistDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.compose.album.AlbumScreen
import us.huseli.thoucylinder.compose.artist.ArtistScreen
import us.huseli.thoucylinder.compose.imports.ImportScreen
import us.huseli.thoucylinder.compose.library.LibraryScreen
import us.huseli.thoucylinder.compose.modalcover.ModalCover
import us.huseli.thoucylinder.compose.modalcover.rememberModalCoverState
import us.huseli.thoucylinder.compose.playlist.PlaylistScreen
import us.huseli.thoucylinder.compose.screens.DebugScreen
import us.huseli.thoucylinder.compose.screens.DownloadsScreen
import us.huseli.thoucylinder.compose.screens.QueueScreen
import us.huseli.thoucylinder.compose.search.SearchScreen
import us.huseli.thoucylinder.compose.settings.SettingsScreen
import us.huseli.thoucylinder.dataclasses.album.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.album.LocalAlbumCallbacks
import us.huseli.thoucylinder.dataclasses.artist.ArtistCallbacks
import us.huseli.thoucylinder.dataclasses.artist.LocalArtistCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.track.LocalTrackCallbacks
import us.huseli.thoucylinder.dataclasses.track.TrackCallbacks
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.RadioViewModel

@Composable
fun App(
    startDestination: String,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
    radioViewModel: RadioViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navController: NavHostController = rememberNavController()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val currentTrackExists by viewModel.currentTrackExists.collectAsStateWithLifecycle()
    val appStartCount by viewModel.appStartCount.collectAsStateWithLifecycle()
    val contentSize by viewModel.contentSize.collectAsStateWithLifecycle()

    var activeMenuItemId by rememberSaveable { mutableStateOf<MenuItemId?>(MenuItemId.LIBRARY) }
    val modalCoverState = rememberModalCoverState(contentSize = contentSize)
    val snackbarBottomPadding = remember(currentTrackExists) { if (currentTrackExists) 80.dp else 0.dp }
    var showWelcomeDialog by rememberSaveable { mutableStateOf(appStartCount <= 1) }

    /** Weird onBackPressedCallback shit begins */
    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                modalCoverState.animateToCollapsed()
            }
        }
    }

    LaunchedEffect(lifecycleOwner, onBackPressedDispatcher) {
        onBackPressedDispatcher?.let { dispatcher ->
            navController.setLifecycleOwner(lifecycleOwner)
            navController.setOnBackPressedDispatcher(dispatcher)
            dispatcher.addCallback(lifecycleOwner, onBackPressedCallback)
        }
    }

    LaunchedEffect(modalCoverState.isExpanded) {
        onBackPressedCallback.isEnabled = modalCoverState.isExpanded
    }
    /** End of weird onBackPressedCallback shit */

    LaunchedEffect(configuration.screenHeightDp, configuration.screenWidthDp) {
        val dpSize = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)

        with(density) { viewModel.setScreenSize(dpSize = dpSize, dpSize.toSize()) }
    }

    val onNavigate = remember {
        { route: String ->
            radioViewModel.setActiveArtistId(null)
            radioViewModel.setActiveAlbumId(null)
            modalCoverState.animateToCollapsed()
            navController.navigate(route)
        }
    }

    val onGotoPlaylistClick = remember {
        { playlistId: String ->
            navController.navigate(PlaylistDestination.route(playlistId))
            modalCoverState.animateToCollapsed()
        }
    }

    val onGotoAlbumClick = remember {
        { albumId: String ->
            navController.navigate(AlbumDestination.route(albumId))
            modalCoverState.animateToCollapsed()
        }
    }

    val dialogCallbacks = rememberDialogCallbacks(
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoPlaylistClick = onGotoPlaylistClick,
        onGotoLibraryClick = { navController.navigate(LibraryDestination.route) },
    )

    val appCallbacks = remember {
        AppCallbacks(
            onBackClick = { if (!navController.popBackStack()) navController.navigate(LibraryDestination.route) },
            onGotoArtistClick = {
                navController.navigate(ArtistDestination.route(it))
                modalCoverState.animateToCollapsed()
            },
            onGotoImportClick = { navController.navigate(ImportDestination.route) },
            onGotoLibraryClick = { navController.navigate(LibraryDestination.route) },
            onGotoPlaylistClick = onGotoPlaylistClick,
            onGotoSearchClick = { navController.navigate(SearchDestination.route) },
            onGotoSettingsClick = { navController.navigate(SettingsDestination.route) },
        )
    }

    val artistCallbacks = remember {
        ArtistCallbacks(
            onPlayClick = { viewModel.playArtist(it) },
            onStartRadioClick = { viewModel.startArtistRadio(it) },
            onEnqueueClick = { viewModel.enqueueArtist(it) },
            onAddToPlaylistClick = dialogCallbacks.onAddArtistToPlaylistClick,
            onGotoArtistClick = appCallbacks.onGotoArtistClick,
        )
    }

    val trackCallbacks = remember {
        TrackCallbacks(
            onAddToPlaylistClick = { dialogCallbacks.onAddTracksToPlaylistClick(listOf(it.trackId)) },
            onDownloadClick = { viewModel.downloadTrack(it.trackId) },
            onEditClick = { dialogCallbacks.onEditTrackClick(it.trackId) },
            onEnqueueClick = { viewModel.enqueueTrack(it.trackId) },
            onGotoAlbumClick = onGotoAlbumClick,
            onGotoArtistClick = appCallbacks.onGotoArtistClick,
            onPlayClick = { viewModel.playTrack(it.trackId) },
            onShowInfoClick = { dialogCallbacks.onShowTrackInfoClick(it.trackId) },
            onStartRadioClick = { viewModel.startTrackRadio(it.trackId) },
        )
    }

    val albumCallbacks = remember {
        AlbumCallbacks(
            onAddToLibraryClick = {
                viewModel.addAlbumToLibrary(
                    albumId = it,
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoLibraryClick = appCallbacks.onGotoLibraryClick,
                )
            },
            onAddToPlaylistClick = { dialogCallbacks.onAddAlbumsToPlaylistClick(listOf(it)) },
            onArtistClick = appCallbacks.onGotoArtistClick,
            onCancelDownloadClick = { viewModel.cancelAlbumDownload(it) },
            onDeleteClick = { dialogCallbacks.onDeleteAlbumsClick(listOf(it)) },
            onDownloadClick = dialogCallbacks.onDownloadAlbumClick,
            onEditClick = dialogCallbacks.onEditAlbumClick,
            onEnqueueClick = { viewModel.enqueueAlbum(it) },
            onGotoAlbumClick = onGotoAlbumClick,
            onPlayClick = { viewModel.playAlbum(it) },
            onStartRadioClick = { viewModel.startAlbumRadio(it) },
        )
    }

    AskMusicImportPermissions()

    if (showWelcomeDialog) {
        WelcomeDialog(onCancel = { showWelcomeDialog = false })
    }

    CompositionLocalProvider(
        LocalAlbumCallbacks provides albumCallbacks,
        LocalAppCallbacks provides appCallbacks,
        LocalAppDialogCallbacks provides dialogCallbacks,
        LocalArtistCallbacks provides artistCallbacks,
        LocalTrackCallbacks provides trackCallbacks,
    ) {
        FistopyScaffold(
            // TODO: Don't know if this is needed:
            // modifier = modifier.safeDrawingPadding(),
            modifier = modifier,
            activeMenuItemId = activeMenuItemId,
            onNavigate = onNavigate,
            snackbarModifier = Modifier.padding(bottom = snackbarBottomPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = if (currentTrackExists) 80.dp else 0.dp)
            ) {
                composable(route = SearchDestination.route) {
                    activeMenuItemId = SearchDestination.menuItemId
                    SearchScreen()
                }

                composable(route = LibraryDestination.route) {
                    activeMenuItemId = LibraryDestination.menuItemId
                    LibraryScreen()
                }

                composable(route = QueueDestination.route) {
                    activeMenuItemId = QueueDestination.menuItemId
                    QueueScreen()
                }

                composable(route = AlbumDestination.routeTemplate, arguments = AlbumDestination.arguments) {
                    activeMenuItemId = null
                    radioViewModel.setActiveAlbumId(it.savedStateHandle.get<String>(NAV_ARG_ALBUM))
                    AlbumScreen()
                }

                composable(route = ArtistDestination.routeTemplate, arguments = ArtistDestination.arguments) {
                    activeMenuItemId = null
                    radioViewModel.setActiveArtistId(it.savedStateHandle.get<String>(NAV_ARG_ARTIST))
                    ArtistScreen()
                }

                composable(route = PlaylistDestination.routeTemplate, arguments = PlaylistDestination.arguments) {
                    activeMenuItemId = null
                    PlaylistScreen()
                }

                composable(route = ImportDestination.route) {
                    activeMenuItemId = ImportDestination.menuItemId
                    ImportScreen()
                }

                if (BuildConfig.DEBUG) {
                    composable(route = DebugDestination.route) {
                        activeMenuItemId = DebugDestination.menuItemId
                        DebugScreen()
                    }
                }

                composable(route = DownloadsDestination.route) {
                    activeMenuItemId = DownloadsDestination.menuItemId
                    DownloadsScreen()
                }

                composable(route = SettingsDestination.route) {
                    activeMenuItemId = SettingsDestination.menuItemId
                    SettingsScreen()
                }
            }

            ModalCover(state = modalCoverState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
