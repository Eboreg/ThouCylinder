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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Artist
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel
import java.util.UUID

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
    var deleteAlbumCombos by rememberSaveable { mutableStateOf<Collection<AbstractAlbumCombo>?>(null) }
    var editAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var editTrackCombo by rememberSaveable { mutableStateOf<AbstractTrackCombo?>(null) }
    var infoTrackCombo by rememberSaveable { mutableStateOf<AbstractTrackCombo?>(null) }
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
        { playlistId: UUID ->
            navController.navigate(PlaylistDestination.route(playlistId))
            isCoverExpanded = false
        }
    }

    val onAlbumClick = remember {
        { albumId: UUID ->
            navController.navigate(AlbumDestination.route(albumId))
            isCoverExpanded = false
        }
    }

    val appCallbacks = remember {
        AppCallbacks(
            onAddAlbumToLibraryClick = { combo ->
                viewModel.addAlbumsToLibrary(listOf(combo.album.albumId))
                SnackbarEngine.addInfo(
                    context.getString(R.string.added_x_to_the_library, combo.album.title)
                        .umlautify()
                )
            },
            onAddToPlaylistClick = { selection ->
                addToPlaylistSelection = selection
                addToPlaylist = true
            },
            onAlbumClick = onAlbumClick,
            onArtistClick = { artistId ->
                navController.navigate(ArtistDestination.route(artistId))
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
            onDownloadAlbumClick = { album -> addDownloadedAlbum = album },
            onDownloadTrackClick = { trackCombo -> viewModel.downloadTrack(trackCombo) },
            onEditAlbumClick = { combo -> editAlbum = combo.album },
            onPlaylistClick = onPlaylistClick,
            onShowTrackInfoClick = { combo -> infoTrackCombo = combo },
            onDeleteAlbumCombosClick = { combos -> deleteAlbumCombos = combos },
            onEditTrackClick = { editTrackCombo = it },
            onStartAlbumRadioClick = { viewModel.startAlbumRadio(it) },
            onStartArtistRadioClick = { viewModel.startArtistRadio(it) },
            onStartTrackRadioClick = { viewModel.startTrackRadio(it) },
        )
    }

    AppCallbacksComposables(
        viewModel = viewModel,
        onCancel = {
            editAlbum = null
            editTrackCombo = null
            deleteAlbumCombos = null
            addDownloadedAlbum = null
            createPlaylist = false
            addToPlaylist = false
            infoTrackCombo = null
            addToPlaylistSelection = null
        },
        onPlaylistClick = onPlaylistClick,
        onAlbumClick = onAlbumClick,
        onOpenCreatePlaylistDialog = {
            addToPlaylist = false
            createPlaylist = true
        },
        editAlbum = editAlbum,
        editTrackCombo = editTrackCombo,
        deleteAlbumCombos = deleteAlbumCombos,
        addDownloadedAlbum = addDownloadedAlbum,
        createPlaylist = createPlaylist,
        addToPlaylist = addToPlaylist,
        addToPlaylistSelection = addToPlaylistSelection,
        infoTrackCombo = infoTrackCombo,
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
        onInnerPaddingChange = { viewModel.setInnerPadding(it) },
        onContentAreaSizeChange = { viewModel.setContentAreaSize(it) },
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

        currentTrackCombo?.also { combo ->
            ModalCover(
                trackCombo = combo,
                viewModel = queueViewModel,
                isExpanded = isCoverExpanded,
                onExpand = { isCoverExpanded = true },
                onCollapse = { isCoverExpanded = false },
                trackCallbacks = TrackCallbacks(
                    appCallbacks = appCallbacks,
                    combo = combo,
                    context = context,
                ),
            )
        }
    }
}
