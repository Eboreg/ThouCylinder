package us.huseli.thoucylinder.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
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
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.RecommendationsDestination
import us.huseli.thoucylinder.SearchDestination
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.compose.album.DeleteAlbumsDialog
import us.huseli.thoucylinder.compose.album.EditAlbumMethodDialog
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
import us.huseli.thoucylinder.compose.screens.settings.LocalMusicUriDialog
import us.huseli.thoucylinder.compose.track.EditTrackDialog
import us.huseli.thoucylinder.compose.track.TrackInfoDialog
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.RadioViewModel
import us.huseli.thoucylinder.viewmodels.RootStateViewModel
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel

@Composable
fun App(
    startDestination: String,
    viewModel: AppViewModel = hiltViewModel(),
    youtubeSearchViewModel: YoutubeSearchViewModel = hiltViewModel(),
    radioViewModel: RadioViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navController: NavHostController = rememberNavController()

    val currentTrackExists by viewModel.currentTrackExists.collectAsStateWithLifecycle()
    val isCoverExpanded by viewModel.isCoverExpanded.collectAsStateWithLifecycle()
    val isWelcomeDialogShown by viewModel.isWelcomeDialogShown.collectAsStateWithLifecycle()
    val umlautify by viewModel.umlautify.collectAsStateWithLifecycle()

    var activeMenuItemId by rememberSaveable { mutableStateOf<MenuItemId?>(MenuItemId.LIBRARY) }

    // This is just to forcefully recompose stuff when the value changes:
    LaunchedEffect(umlautify) {}

    /** Weird onBackPressedCallback shit begins */
    val onBackPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.collapseCover()
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
            radioViewModel.setActiveArtistId(null)
            radioViewModel.setActiveAlbumId(null)
            viewModel.collapseCover()
            navController.navigate(route)
        }
    }

    val onPlaylistClick = remember {
        { playlistId: String ->
            navController.navigate(PlaylistDestination.route(playlistId))
            viewModel.collapseCover()
        }
    }

    val onAlbumClick = remember {
        { albumId: String ->
            navController.navigate(AlbumDestination.route(albumId))
            viewModel.collapseCover()
        }
    }

    val dialogCallbaks = rememberDialogCallbacks(
        onAlbumClick = onAlbumClick,
        onPlaylistClick = onPlaylistClick,
    )

    val appCallbacks = remember {
        AppCallbacks(
            onAddAlbumsToPlaylistClick = dialogCallbaks.onAddAlbumsToPlaylistClick,
            onAddTracksToPlaylistClick = dialogCallbaks.onAddTracksToPlaylistClick,
            onArtistClick = {
                navController.navigate(ArtistDestination.route(it))
                viewModel.collapseCover()
            },
            onBackClick = { if (!navController.popBackStack()) navController.navigate(LibraryDestination.route) },
            onCreatePlaylistClick = dialogCallbaks.onCreatePlaylistClick,
            onDeleteAlbumsClick = dialogCallbaks.onDeleteAlbumsClick,
            onDeletePlaylistClick = { playlistId ->
                viewModel.deletePlaylist(
                    playlistId = playlistId,
                    context = context,
                    onRestored = { onPlaylistClick(playlistId) },
                )
            },
            onPlaylistClick = onPlaylistClick,
            onStartArtistRadioClick = { viewModel.startArtistRadio(it) },
        )
    }

    val trackCallbacks = remember {
        TrackCallbacks(
            onAddToPlaylistClick = { dialogCallbaks.onAddTracksToPlaylistClick(listOf(it)) },
            onDownloadClick = { viewModel.downloadTrack(it) },
            onEditClick = dialogCallbaks.onEditTrackClick,
            onGotoArtistClick = appCallbacks.onArtistClick,
            onShowInfoClick = dialogCallbaks.onShowTrackInfoClick,
            onStartRadioClick = { viewModel.startTrackRadio(it) },
            onGotoAlbumClick = { viewModel.getAlbumIdByTrackId(it, onAlbumClick) },
            onEnqueueClick = { viewModel.enqueueTrack(it) },
            onTrackClick = { viewModel.playTrack(it) },
        )
    }

    val albumCallbacks = remember {
        AlbumCallbacks(
            onAddToLibraryClick = {},
            onAddToPlaylistClick = { dialogCallbaks.onAddAlbumsToPlaylistClick(listOf(it)) },
            onAlbumClick = onAlbumClick,
            onArtistClick = appCallbacks.onArtistClick,
            onCancelDownloadClick = { viewModel.cancelAlbumDownload(it) },
            onDeleteClick = { dialogCallbaks.onDeleteAlbumsClick(listOf(it)) },
            onDownloadClick = dialogCallbaks.onDownloadAlbumClick,
            onEditClick = dialogCallbaks.onEditAlbumClick,
            onStartRadioClick = { viewModel.startAlbumRadio(it) },
        )
    }

    AskMusicImportPermissions()

    if (!isWelcomeDialogShown) {
        WelcomeDialog(onCancel = { viewModel.setWelcomeDialogShown(true) })
    }

    ThouCylinderScaffold(
        modifier = Modifier.safeDrawingPadding(),
        activeMenuItemId = activeMenuItemId,
        onNavigate = onNavigate,
        onRadioClick = dialogCallbaks.onRadioClick,
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = if (currentTrackExists) 80.dp else 0.dp)
        ) {
            composable(route = SearchDestination.route) {
                activeMenuItemId = SearchDestination.menuItemId
                YoutubeSearchScreen(
                    viewModel = youtubeSearchViewModel,
                    appCallbacks = appCallbacks,
                    albumCallbacks = albumCallbacks,
                    trackCallbacks = trackCallbacks,
                )
            }

            composable(route = LibraryDestination.route) {
                activeMenuItemId = LibraryDestination.menuItemId
                LibraryScreen(
                    appCallbacks = appCallbacks,
                    albumCallbacks = albumCallbacks,
                    trackCallbacks = trackCallbacks,
                )
            }

            composable(route = QueueDestination.route) {
                activeMenuItemId = QueueDestination.menuItemId
                QueueScreen(
                    trackCallbacks = trackCallbacks,
                    appCallbacks = appCallbacks,
                )
            }

            composable(route = AlbumDestination.routeTemplate, arguments = AlbumDestination.arguments) {
                activeMenuItemId = null
                radioViewModel.setActiveAlbumId(it.savedStateHandle.get<String>(NAV_ARG_ALBUM))
                AlbumScreen(
                    appCallbacks = appCallbacks,
                    albumCallbacks = albumCallbacks,
                    trackCallbacks = trackCallbacks,
                )
            }

            composable(route = ArtistDestination.routeTemplate, arguments = ArtistDestination.arguments) {
                activeMenuItemId = null
                radioViewModel.setActiveArtistId(it.savedStateHandle.get<String>(NAV_ARG_ARTIST))
                ArtistScreen(
                    appCallbacks = appCallbacks,
                    albumCallbacks = albumCallbacks,
                    trackCallbacks = trackCallbacks,
                )
            }

            composable(route = PlaylistDestination.routeTemplate, arguments = PlaylistDestination.arguments) {
                activeMenuItemId = null
                PlaylistScreen(
                    appCallbacks = appCallbacks,
                    trackCallbacks = trackCallbacks,
                )
            }

            composable(route = ImportDestination.route) {
                activeMenuItemId = ImportDestination.menuItemId
                ImportScreen(
                    onGotoLibraryClick = remember { { navController.navigate(LibraryDestination.route) } },
                    onGotoSettingsClick = remember { { navController.navigate(SettingsDestination.route) } },
                    onGotoAlbumClick = onAlbumClick,
                    onGotoSearchClick = remember { { navController.navigate(SearchDestination.route) } },
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
                SettingsScreen(onBackClick = appCallbacks.onBackClick)
            }

            composable(route = RecommendationsDestination.route) {
                activeMenuItemId = RecommendationsDestination.menuItemId
                RecommendationsScreen()
            }
        }

        ModalCover(
            isExpanded = isCoverExpanded,
            onExpand = remember { { viewModel.expandCover() } },
            onCollapse = remember { { viewModel.collapseCover() } },
            trackCallbacks = trackCallbacks,
        )
    }
}


@Composable
inline fun rememberDialogCallbacks(
    crossinline onAlbumClick: @DisallowComposableCalls (String) -> Unit,
    crossinline onPlaylistClick: @DisallowComposableCalls (String) -> Unit,
    viewModel: RootStateViewModel = hiltViewModel(),
): AppDialogCallbacks {
    val context = LocalContext.current

    val addToPlaylistTrackIds by viewModel.addToPlaylistTrackIds.collectAsStateWithLifecycle()
    val albumToDownload by viewModel.albumToDownload.collectAsStateWithLifecycle()
    val createPlaylistActive by viewModel.createPlaylistActive.collectAsStateWithLifecycle()
    val deleteAlbumIds by viewModel.deleteAlbums.collectAsStateWithLifecycle()
    val editAlbumId by viewModel.editAlbumId.collectAsStateWithLifecycle()
    val editTrackState by viewModel.editTrackState.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    val showInfoTrackCombo by viewModel.showInfoTrackCombo.collectAsStateWithLifecycle()
    val showLibraryRadioDialog by viewModel.showLibraryRadioDialog.collectAsStateWithLifecycle()

    addToPlaylistTrackIds.takeIf { it.isNotEmpty() }?.also { trackIds ->
        PlaylistDialog(
            trackIds = trackIds.toImmutableList(),
            onPlaylistClick = { onPlaylistClick(it) },
            onClose = { viewModel.setAddToPlaylistTrackIds(emptyList()) },
        )
    }

    albumToDownload?.also { album ->
        if (localMusicUri == null) {
            LocalMusicUriDialog(
                onSave = { viewModel.setLocalMusicUri(it) },
                text = { Text(stringResource(R.string.you_need_to_select_your_local_music_root_folder)) },
            )
        } else {
            viewModel.setAlbumToDownloadId(null)
            viewModel.downloadAlbum(
                albumId = album.albumId,
                onFinish = { hasErrors ->
                    SnackbarEngine.addInfo(
                        message = if (hasErrors)
                            context.getString(R.string.album_was_downloaded_with_errors, album).umlautify()
                        else context.getString(R.string.album_was_downloaded, album).umlautify(),
                        actionLabel = context.getString(R.string.go_to_album).umlautify(),
                        onActionPerformed = { onAlbumClick(album.albumId) },
                    )
                },
            ) { track, throwable ->
                SnackbarEngine.addError("Error on downloading $track: $throwable")
            }
        }
    }

    if (createPlaylistActive) {
        CreatePlaylistDialog(
            onSave = { name ->
                viewModel.createPlaylist(name = name, onFinish = { onPlaylistClick(it) })
                viewModel.setCreatePlaylistActive(false)
            },
            onCancel = { viewModel.setCreatePlaylistActive(false) },
        )
    }

    deleteAlbumIds.takeIf { it.isNotEmpty() }?.also { albumIds ->
        DeleteAlbumsDialog(albumIds = albumIds, onClose = { viewModel.setDeleteAlbums(emptyList()) })
    }

    editAlbumId?.also {
        EditAlbumMethodDialog(
            albumId = it,
            onClose = { viewModel.setEditAlbumId(null) },
        )
    }

    editTrackState?.also { EditTrackDialog(state = it, onClose = { viewModel.setEditTrackId(null) }) }

    showInfoTrackCombo?.also {
        TrackInfoDialog(
            track = it.track,
            albumTitle = it.album?.title,
            albumArtist = it.albumArtists.joined(),
            year = it.year,
            localPath = it.localPath,
            onClose = { viewModel.setShowInfoTrackCombo(null) },
        )
    }

    if (showLibraryRadioDialog) {
        RadioDialog(onDismissRequest = { viewModel.setShowLibraryRadioDialog(false) })
    }

    return remember {
        AppDialogCallbacks(
            onAddAlbumsToPlaylistClick = { viewModel.setAddToPlaylistAlbumIds(it) },
            onAddTracksToPlaylistClick = { viewModel.setAddToPlaylistTrackIds(it) },
            onCreatePlaylistClick = { viewModel.setCreatePlaylistActive(true) },
            onDeleteAlbumsClick = { viewModel.setDeleteAlbums(it) },
            onDownloadAlbumClick = { viewModel.setAlbumToDownloadId(it) },
            onEditAlbumClick = { viewModel.setEditAlbumId(it) },
            onEditTrackClick = { viewModel.setEditTrackId(it) },
            onShowTrackInfoClick = { viewModel.setShowInfoTrackCombo(it) },
            onRadioClick = { viewModel.setShowLibraryRadioDialog(true) },
        )
    }
}
