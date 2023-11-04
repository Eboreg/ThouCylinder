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
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.dataclasses.entities.Track
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
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
    val currentPojo by queueViewModel.playerCurrentPojo.collectAsStateWithLifecycle()

    var activeScreen by rememberSaveable { mutableStateOf<String?>("library") }
    var addToPlaylistSelection by rememberSaveable { mutableStateOf<Selection?>(null) }
    var infoDialogTrack by rememberSaveable { mutableStateOf<Track?>(null) }
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    var isCreatePlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addDownloadedAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var addAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }
    var editAlbumDialogAlbum by rememberSaveable { mutableStateOf<Album?>(null) }

    LaunchedEffect(Unit) {
        viewModel.importNewMediaStoreAlbums(context = context)
        viewModel.deleteOrphanTracksAndAlbums()
        viewModel.deleteTempTracksAndAlbums()
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
        onBackClick = { navController.popBackStack() },
        onCancelAlbumDownloadClick = { viewModel.cancelAlbumDownload(it) },
        onCreatePlaylistClick = { isCreatePlaylistDialogOpen = true },
        onDeletePlaylistClick = { pojo ->
            viewModel.deletePlaylist(pojo) {
                SnackbarEngine.addInfo(
                    message = context.getString(R.string.the_playlist_was_deleted),
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
        onShowTrackInfoClick = { track -> infoDialogTrack = track },
    )

    val displayAddedToPlaylistMessage: (UUID) -> Unit = { playlistId ->
        SnackbarEngine.addInfo(
            message = context.getString(R.string.selection_was_added_to_playlist),
            actionLabel = context.getString(R.string.go_to_playlist),
            onActionPerformed = { appCallbacks.onPlaylistClick(playlistId) },
        )
    }

    infoDialogTrack?.also { track ->
        var metadata by rememberSaveable { mutableStateOf(track.metadata) }
        var album by rememberSaveable { mutableStateOf<Album?>(null) }

        LaunchedEffect(Unit) {
            if (metadata == null) metadata = viewModel.getTrackMetadata(track)
            album = viewModel.getTrackAlbum(track)
        }

        TrackInfoDialog(
            isDownloaded = track.isDownloaded,
            isOnYoutube = track.isOnYoutube,
            metadata = metadata,
            albumTitle = album?.title,
            albumArtist = album?.artist,
            year = track.year ?: album?.year,
            onClose = { infoDialogTrack = null },
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
                val playlist = Playlist(name = name)

                isCreatePlaylistDialogOpen = false
                viewModel.createPlaylist(playlist, addToPlaylistSelection)

                if (addToPlaylistSelection != null) {
                    displayAddedToPlaylistMessage(playlist.playlistId)
                    addToPlaylistSelection = null
                } else appCallbacks.onPlaylistClick(playlist.playlistId)
            },
            onCancel = { isCreatePlaylistDialogOpen = false },
        )
    }

    addDownloadedAlbumDialogAlbum?.also { album ->
        EditAlbumDialog(
            initialAlbum = album,
            title = stringResource(R.string.add_album_to_library),
            onCancel = { addDownloadedAlbumDialogAlbum = null },
            onSave = {
                addDownloadedAlbumDialogAlbum = null
                viewModel.downloadAndSaveAlbum(it)
            },
        )
    }

    addAlbumDialogAlbum?.also { album ->
        EditAlbumDialog(
            initialAlbum = album,
            title = stringResource(R.string.add_album_to_library),
            onCancel = { addAlbumDialogAlbum = null },
            onSave = {
                addAlbumDialogAlbum = null
                viewModel.saveAlbumWithTracks(it)
                viewModel.tagAlbumTracks(it)
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
                        appCallbacks = appCallbacks,
                    )
                }

                composable(route = LibraryDestination.route) {
                    activeScreen = "library"
                    LibraryScreen(appCallbacks = appCallbacks)
                }

                composable(route = QueueDestination.route) {
                    activeScreen = "queue"
                    QueueScreen(appCallbacks = appCallbacks)
                }

                composable(
                    route = AlbumDestination.routeTemplate,
                    arguments = AlbumDestination.arguments,
                ) {
                    activeScreen = null
                    AlbumScreen(appCallbacks = appCallbacks)
                }

                composable(
                    route = ArtistDestination.routeTemplate,
                    arguments = ArtistDestination.arguments,
                ) {
                    activeScreen = null
                    ArtistScreen(appCallbacks = appCallbacks)
                }

                composable(
                    route = PlaylistDestination.routeTemplate,
                    arguments = PlaylistDestination.arguments,
                ) {
                    activeScreen = null
                    PlaylistScreen(appCallbacks = appCallbacks)
                }
            }

            currentPojo?.also { pojo ->
                ModalCover(
                    pojo = pojo,
                    viewModel = queueViewModel,
                    isExpanded = isCoverExpanded,
                    onExpand = { isCoverExpanded = true },
                    onCollapse = { isCoverExpanded = false },
                    trackCallbacks = TrackCallbacks(
                        onAddToPlaylistClick = { appCallbacks.onAddToPlaylistClick(Selection(track = pojo.track)) },
                        onDownloadClick = { appCallbacks.onDownloadTrackClick(pojo.track) },
                        onShowInfoClick = { appCallbacks.onShowTrackInfoClick(pojo.track) },
                        onAlbumClick = pojo.album?.albumId?.let { { appCallbacks.onAlbumClick(it) } },
                        onArtistClick = pojo.artist?.let { { appCallbacks.onArtistClick(it) } },
                    ),
                )
            }
        }
    }
}
