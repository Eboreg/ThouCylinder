package us.huseli.thoucylinder.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.AddDestination
import us.huseli.thoucylinder.AlbumDestination
import us.huseli.thoucylinder.ArtistDestination
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.MenuItemId
import us.huseli.thoucylinder.PlaylistDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.compose.album.DeleteAlbumDialog
import us.huseli.thoucylinder.compose.album.EditAlbumDialog
import us.huseli.thoucylinder.compose.screens.AlbumScreen
import us.huseli.thoucylinder.compose.screens.ArtistScreen
import us.huseli.thoucylinder.compose.screens.DebugScreen
import us.huseli.thoucylinder.compose.screens.DownloadsScreen
import us.huseli.thoucylinder.compose.screens.ImportScreen
import us.huseli.thoucylinder.compose.screens.LibraryScreen
import us.huseli.thoucylinder.compose.screens.LocalMusicDownloadUriDialog
import us.huseli.thoucylinder.compose.screens.PlaylistScreen
import us.huseli.thoucylinder.compose.screens.QueueScreen
import us.huseli.thoucylinder.compose.screens.SettingsScreen
import us.huseli.thoucylinder.compose.screens.YoutubeSearchScreen
import us.huseli.thoucylinder.compose.track.TrackInfoDialog
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import us.huseli.thoucylinder.viewmodels.YoutubeSearchViewModel
import java.util.UUID

@Composable
fun App(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
    youtubeSearchViewModel: YoutubeSearchViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
    startDestination: String = LibraryDestination.route,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = LocalContext.current
    val currentPojo by queueViewModel.playerCurrentPojo.collectAsStateWithLifecycle(null)
    val musicDownloadUri by viewModel.musicDownloadUri.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var activeMenuItemId by rememberSaveable { mutableStateOf<MenuItemId?>(MenuItemId.LIBRARY) }
    var addToPlaylistSelection by rememberSaveable { mutableStateOf<Selection?>(null) }
    var infoDialogTrackPojo by rememberSaveable { mutableStateOf<AbstractTrackPojo?>(null) }
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    var isCreatePlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addDownloadedAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var addAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var editAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var deleteAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }

    LaunchedEffect(Unit) {
        viewModel.doStartupTasks(context)
    }

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

    val onPlaylistClick = { playlistId: UUID ->
        navController.navigate(PlaylistDestination.route(playlistId))
        isCoverExpanded = false
    }

    val appCallbacks = AppCallbacks(
        onAddAlbumToLibraryClick = { album -> addAlbumDialogAlbum = album },
        onAddToPlaylistClick = { selection ->
            addToPlaylistSelection = selection
            isAddToPlaylistDialogOpen = true
        },
        onAlbumClick = { albumId ->
            navController.navigate(AlbumDestination.route(albumId))
            isCoverExpanded = false
        },
        onArtistClick = { artist ->
            navController.navigate(ArtistDestination.route(artist))
            isCoverExpanded = false
        },
        onBackClick = { if (!navController.popBackStack()) navController.navigate(LibraryDestination.route) },
        onCancelAlbumDownloadClick = { viewModel.cancelAlbumDownload(it) },
        onCreatePlaylistClick = { isCreatePlaylistDialogOpen = true },
        onDeletePlaylistClick = { pojo ->
            viewModel.deletePlaylist(pojo) {
                SnackbarEngine.addInfo(
                    message = context.getString(R.string.the_playlist_was_removed),
                    actionLabel = context.getString(R.string.undo),
                    onActionPerformed = {
                        viewModel.undoDeletePlaylist { pojo ->
                            SnackbarEngine.addInfo(
                                message = context.getString(R.string.the_playlist_was_restored),
                                actionLabel = context.getString(R.string.go_to_playlist),
                                onActionPerformed = { onPlaylistClick(pojo.playlistId) },
                            )
                        }
                    }
                )
            }
        },
        onDownloadAlbumClick = { album -> addDownloadedAlbumDialogAlbum = album },
        onDownloadTrackClick = { track -> viewModel.downloadTrack(track) },
        onEditAlbumClick = { album -> editAlbumDialogAlbum = album },
        onPlaylistClick = onPlaylistClick,
        onShowTrackInfoClick = { pojo -> infoDialogTrackPojo = pojo },
        onDeleteAlbumClick = { album -> deleteAlbumDialogAlbum = album },
        onRemoveAlbumFromLibraryClick = { album ->
            viewModel.markAlbumForDeletion(album)
            SnackbarEngine.addInfo(
                message = context.getString(R.string.removed_album_from_library, album.title),
                actionLabel = context.getString(R.string.undo),
                onActionPerformed = { viewModel.unmarkAlbumForDeletion(album) },
            )
        },
    )

    val displayAddedToPlaylistMessage: (UUID) -> Unit = { playlistId ->
        SnackbarEngine.addInfo(
            message = context.getString(R.string.selection_was_added_to_playlist),
            actionLabel = context.getString(R.string.go_to_playlist),
            onActionPerformed = { appCallbacks.onPlaylistClick(playlistId) },
        )
    }

    infoDialogTrackPojo?.also { pojo ->
        var album by rememberSaveable { mutableStateOf(pojo.album) }
        var localPath by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            viewModel.ensureTrackMetadata(pojo.track, commit = true)
            album = pojo.album ?: viewModel.getTrackAlbum(pojo.track)
            localPath = pojo.track.getLocalAbsolutePath(context)
        }

        TrackInfoDialog(
            isDownloaded = pojo.track.isDownloaded,
            isOnYoutube = pojo.track.isOnYoutube,
            metadata = pojo.track.metadata,
            albumTitle = album?.title,
            albumArtist = album?.artist,
            year = pojo.track.year ?: album?.year,
            localPath = localPath,
            onClose = { infoDialogTrackPojo = null },
            isOnSpotify = pojo.isOnSpotify,
        )
    }

    if (isAddToPlaylistDialogOpen) {
        addToPlaylistSelection?.also { selection ->
            val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())

            AddToPlaylistDialog(
                playlists = playlists,
                onSelect = { playlistPojo ->
                    viewModel.addSelectionToPlaylist(selection, playlistPojo)
                    isAddToPlaylistDialogOpen = false
                    addToPlaylistSelection = null
                    displayAddedToPlaylistMessage(playlistPojo.playlistId)
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
                val playlistPojo = PlaylistPojo(name = name)

                isCreatePlaylistDialogOpen = false
                viewModel.createPlaylist(playlistPojo, addToPlaylistSelection)

                if (addToPlaylistSelection != null) {
                    displayAddedToPlaylistMessage(playlistPojo.playlistId)
                    addToPlaylistSelection = null
                } else appCallbacks.onPlaylistClick(playlistPojo.playlistId)
            },
            onCancel = { isCreatePlaylistDialogOpen = false },
        )
    }

    addDownloadedAlbumDialogAlbum?.also { album ->
        fun download(pojo: AlbumWithTracksPojo) {
            viewModel.downloadAlbum(
                pojo = pojo,
                context = context,
                onTrackError = { track, throwable ->
                    SnackbarEngine.addError("Error on downloading $track: $throwable")
                },
                onFinish = { hasErrors ->
                    SnackbarEngine.addInfo(
                        message = if (hasErrors)
                            context.getString(R.string.album_was_downloaded_with_errors, pojo.album)
                        else context.getString(R.string.album_was_downloaded, pojo.album),
                        actionLabel = context.getString(R.string.go_to_album),
                        onActionPerformed = { appCallbacks.onAlbumClick(pojo.album.albumId) },
                    )
                },
            )
        }

        if (musicDownloadUri == null) {
            LocalMusicDownloadUriDialog(onSave = { viewModel.setMusicDownloadUri(it) })
        } else {
            if (!album.isInLibrary) {
                EditAlbumDialog(
                    initialAlbum = album,
                    title = stringResource(R.string.add_album_to_library),
                    onCancel = { addDownloadedAlbumDialogAlbum = null },
                    onSave = { pojo ->
                        addDownloadedAlbumDialogAlbum = null
                        viewModel.saveAlbumWithTracks(pojo)
                        download(pojo)
                    },
                )
            } else {
                addDownloadedAlbumDialogAlbum = null
                scope.launch { viewModel.getAlbumWithTracks(album.albumId)?.also { pojo -> download(pojo) } }
            }
        }
    }

    addAlbumDialogAlbum?.also { album ->
        EditAlbumDialog(
            initialAlbum = album,
            title = stringResource(R.string.add_album_to_library),
            onCancel = { addAlbumDialogAlbum = null },
            onSave = {
                addAlbumDialogAlbum = null
                viewModel.saveAlbumWithTracks(it)
            },
        )
    }

    editAlbumDialogAlbum?.also { album ->
        EditAlbumDialog(
            initialAlbum = album,
            title = stringResource(R.string.update_album),
            onCancel = { editAlbumDialogAlbum = null },
            onSave = {
                editAlbumDialogAlbum = null
                viewModel.saveAlbumWithTracks(it)
                viewModel.tagAlbumTracks(it)
            }
        )
    }

    deleteAlbumDialogAlbum?.also { album ->
        DeleteAlbumDialog(
            album = album,
            onCancel = { deleteAlbumDialogAlbum = null },
            onDeleteAlbumAndFilesClick = {
                deleteAlbumDialogAlbum = null
                viewModel.deleteAlbumAndFiles(album) {
                    SnackbarEngine.addInfo(context.getString(R.string.the_album_was_deleted))
                }
            },
            onDeleteFilesClick = {
                deleteAlbumDialogAlbum = null
                viewModel.deleteLocalFiles(album) {
                    SnackbarEngine.addInfo(context.getString(R.string.the_albums_local_files_were_deleted))
                }
            },
        )
    }

    AskMusicImportPermissions()

    ThouCylinderScaffold(
        modifier = modifier,
        activeMenuItemId = activeMenuItemId,
        onNavigate = { route ->
            isCoverExpanded = false
            navController.navigate(route)
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = if (currentPojo != null) 80.dp else 0.dp)
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
                AlbumScreen(appCallbacks = appCallbacks)
            }

            composable(route = ArtistDestination.routeTemplate, arguments = ArtistDestination.arguments) {
                activeMenuItemId = null
                ArtistScreen(appCallbacks = appCallbacks)
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
        }

        currentPojo?.also { pojo ->
            ModalCover(
                pojo = pojo,
                viewModel = queueViewModel,
                isExpanded = isCoverExpanded,
                onExpand = { isCoverExpanded = true },
                onCollapse = { isCoverExpanded = false },
                trackCallbacks = TrackCallbacks.fromAppCallbacks(
                    appCallbacks = appCallbacks,
                    pojo = pojo,
                    context = context,
                )
            )
        }
    }
}
