package us.huseli.thoucylinder.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.toImmutableList
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AddDestination
import us.huseli.thoucylinder.AlbumDestination
import us.huseli.thoucylinder.ArtistDestination
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.PlaylistDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.RecommendationsDestination
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.compose.modalcover.ModalCover
import us.huseli.thoucylinder.compose.screens.AlbumScreen
import us.huseli.thoucylinder.compose.screens.ArtistScreen
import us.huseli.thoucylinder.compose.screens.DebugScreen
import us.huseli.thoucylinder.compose.screens.DownloadsScreen
import us.huseli.thoucylinder.compose.screens.ImportScreen
import us.huseli.thoucylinder.compose.screens.LibraryScreen
import us.huseli.thoucylinder.compose.screens.PlaylistScreen
import us.huseli.thoucylinder.compose.screens.QueueScreen
import us.huseli.thoucylinder.compose.screens.RecommendationsScreen
import us.huseli.thoucylinder.compose.screens.SettingsScreen
import us.huseli.thoucylinder.compose.screens.YoutubeSearchScreen
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel

@Composable
fun App(
    startDestination: String,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
    youtubeSearchViewModel: YoutubeSearchViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navController: NavHostController = rememberNavController()

    val activeRadio by viewModel.activeRadio.collectAsStateWithLifecycle()
    val currentTrackCombo by queueViewModel.currentCombo.collectAsStateWithLifecycle(null)
    val isWelcomeDialogShown by viewModel.isWelcomeDialogShown.collectAsStateWithLifecycle()
    val libraryRadioNovelty by viewModel.libraryRadioNovelty.collectAsStateWithLifecycle()
    val umlautify by viewModel.umlautify.collectAsStateWithLifecycle()

    var activeAlbumCombo by rememberSaveable { mutableStateOf<AbstractAlbumCombo?>(null) }
    var activeArtist by rememberSaveable { mutableStateOf<Artist?>(null) }
    var activeMenuItemId by rememberSaveable { mutableStateOf<MenuItemId?>(MenuItemId.LIBRARY) }
    var addDownloadedAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var addToPlaylist by rememberSaveable { mutableStateOf(false) }
    var addToPlaylistSelection by rememberSaveable { mutableStateOf<Selection?>(null) }
    var createPlaylist by rememberSaveable { mutableStateOf(false) }
    var deleteAlbums by rememberSaveable { mutableStateOf<Collection<Album.ViewState>?>(null) }
    var editAlbum by rememberSaveable { mutableStateOf<Album.ViewState?>(null) }
    var editTrack by rememberSaveable { mutableStateOf<Track.ViewState?>(null) }
    var infoTrack by rememberSaveable { mutableStateOf<Track?>(null) }
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    var isRadioDialogOpen by rememberSaveable { mutableStateOf(false) }

    // This is just to forcefully recompose stuff when the value changes:
    LaunchedEffect(umlautify) {}

    /** Weird onBackPressedCallback shit begin */
    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isCoverExpanded = false
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

    LaunchedEffect(isCoverExpanded) {
        onBackPressedCallback.isEnabled = isCoverExpanded
    }
    /** End of weird onBackPressedCallback shit */

    val onNavigate = remember {
        { route: String ->
            activeArtist = null
            activeAlbumCombo = null
            isCoverExpanded = false
            navController.navigate(route)
        }
    }

    val onPlaylistClick = remember {
        { playlistId: String ->
            navController.navigate(PlaylistDestination.route(playlistId))
            isCoverExpanded = false
        }
    }

    val onAlbumClick = remember {
        { albumId: String ->
            navController.navigate(AlbumDestination.route(albumId))
            isCoverExpanded = false
        }
    }

    val appCallbacks = remember {
        AppCallbacks(
            onAddAlbumToLibraryClick = { state ->
                viewModel.addAlbumsToLibrary(listOf(state.album.albumId))
                SnackbarEngine.addInfo(
                    context.getString(R.string.added_x_to_the_library, state.album.title)
                        .umlautify()
                )
            },
            onAddToPlaylistClick = { selection ->
                addToPlaylistSelection = selection
                addToPlaylist = true
            },
            onAlbumClick = onAlbumClick,
            onArtistClick = {
                navController.navigate(ArtistDestination.route(it))
                isCoverExpanded = false
            },
            onBackClick = { if (!navController.popBackStack()) navController.navigate(LibraryDestination.route) },
            onCancelAlbumDownloadClick = { viewModel.cancelAlbumDownload(it) },
            onCreatePlaylistClick = { createPlaylist = true },
            onDeletePlaylistClick = { playlist ->
                viewModel.deletePlaylist(playlist) {
                    SnackbarEngine.addInfo(
                        message = context.getString(R.string.the_playlist_was_removed).umlautify(),
                        actionLabel = context.getString(R.string.undo).umlautify(),
                        onActionPerformed = {
                            viewModel.undoDeletePlaylist { playlistId ->
                                SnackbarEngine.addInfo(
                                    message = context.getString(R.string.the_playlist_was_restored).umlautify(),
                                    actionLabel = context.getString(R.string.go_to_playlist).umlautify(),
                                    onActionPerformed = { onPlaylistClick(playlistId) },
                                )
                            }
                        }
                    )
                }
            },
            onDownloadAlbumClick = { addDownloadedAlbum = it },
            onDownloadTrackClick = { viewModel.downloadTrack(it) },
            onEditAlbumClick = { editAlbum = it },
            onPlaylistClick = onPlaylistClick,
            onShowTrackInfoClick = { combo -> infoTrack = combo },
            onDeleteAlbumsClick = { states -> deleteAlbums = states },
            onEditTrackClick = { editTrack = it },
            onStartAlbumRadioClick = { viewModel.startAlbumRadio(it) },
            onStartArtistRadioClick = { viewModel.startArtistRadio(it) },
            onStartTrackRadioClick = { viewModel.startTrackRadio(it) },
        )
    }

    AppCallbacksComposables(
        viewModel = viewModel,
        onCancel = {
            editAlbum = null
            editTrack = null
            deleteAlbums = null
            addDownloadedAlbum = null
            createPlaylist = false
            addToPlaylist = false
            infoTrack = null
            addToPlaylistSelection = null
        },
        onPlaylistClick = onPlaylistClick,
        onAlbumClick = onAlbumClick,
        onOpenCreatePlaylistDialog = {
            addToPlaylist = false
            createPlaylist = true
        },
        editAlbum = editAlbum,
        editTrack = editTrack,
        deleteAlbums = deleteAlbums?.toImmutableList(),
        addDownloadedAlbum = addDownloadedAlbum,
        createPlaylist = createPlaylist,
        addToPlaylist = addToPlaylist,
        addToPlaylistSelection = addToPlaylistSelection,
        infoTrack = infoTrack,
    )

    AskMusicImportPermissions()

    if (!isWelcomeDialogShown) {
        WelcomeDialog(onCancel = { viewModel.setWelcomeDialogShown(true) })
    }

    if (isRadioDialogOpen) {
        RadioDialog(
            activeRadio = activeRadio,
            activeTrackCombo = currentTrackCombo,
            activeAlbumCombo = activeAlbumCombo,
            activeArtist = activeArtist,
            libraryRadioNovelty = libraryRadioNovelty,
            onDeactivateClick = { viewModel.deactivateRadio() },
            onStartLibraryRadioClick = { viewModel.startLibraryRadio() },
            onStartArtistRadioClick = { viewModel.startArtistRadio(it) },
            onStartAlbumRadioClick = { viewModel.startAlbumRadio(it) },
            onStartTrackRadioClick = { viewModel.startTrackRadio(it) },
            onDismissRequest = { isRadioDialogOpen = false },
            onLibraryRadioNoveltyChange = { viewModel.setLibraryRadioNovelty(it) },
        )
    }

    ThouCylinderScaffold(
        modifier = modifier,
        activeMenuItemId = activeMenuItemId,
        onNavigate = onNavigate,
        onRadioClick = { isRadioDialogOpen = true },
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = if (currentTrackCombo != null) 80.dp else 0.dp)
        ) {
            composable(route = AddDestination.route) {
                activeMenuItemId = AddDestination.menuItemId
                YoutubeSearchScreen(viewModel = youtubeSearchViewModel, appCallbacks = appCallbacks)
            }

            composable(route = LibraryDestination.route) {
                activeMenuItemId = LibraryDestination.menuItemId
                LibraryScreen(appCallbacks = appCallbacks)
            }

            composable(route = QueueDestination.route) {
                activeMenuItemId = QueueDestination.menuItemId
                QueueScreen(appCallbacks = appCallbacks)
            }

            composable(route = AlbumDestination.routeTemplate, arguments = AlbumDestination.arguments) {
                activeMenuItemId = null
                AlbumScreen(
                    appCallbacks = appCallbacks,
                    onAlbumComboFetched = { activeAlbumCombo = it },
                )
            }

            composable(route = ArtistDestination.routeTemplate, arguments = ArtistDestination.arguments) {
                activeMenuItemId = null
                ArtistScreen(
                    appCallbacks = appCallbacks,
                    onArtistFetched = { activeArtist = it },
                )
            }

            composable(route = PlaylistDestination.routeTemplate, arguments = PlaylistDestination.arguments) {
                activeMenuItemId = null
                PlaylistScreen(appCallbacks = appCallbacks)
            }

            composable(route = ImportDestination.route) {
                activeMenuItemId = ImportDestination.menuItemId
                ImportScreen(
                    onGotoLibraryClick = { navController.navigate(LibraryDestination.route) },
                    onGotoSettingsClick = { navController.navigate(SettingsDestination.route) },
                    onGotoAlbumClick = onAlbumClick,
                )
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
                SettingsScreen(appCallbacks = appCallbacks)
            }

            composable(route = RecommendationsDestination.route) {
                activeMenuItemId = RecommendationsDestination.menuItemId
                RecommendationsScreen()
            }
        }

        currentTrackCombo?.getViewState()?.also { state ->
            ModalCover(
                state = state,
                viewModel = queueViewModel,
                isExpanded = isCoverExpanded,
                onExpand = { isCoverExpanded = true },
                onCollapse = { isCoverExpanded = false },
                trackCallbacks = TrackCallbacks(appCallbacks = appCallbacks, state = state),
            )
        }
    }
}
