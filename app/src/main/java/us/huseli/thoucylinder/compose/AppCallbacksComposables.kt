package us.huseli.thoucylinder.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.album.DeleteAlbumsDialog
import us.huseli.thoucylinder.compose.album.EditAlbumMethodDialog
import us.huseli.thoucylinder.compose.screens.LocalMusicUriDialog
import us.huseli.thoucylinder.compose.track.EditTrackDialog
import us.huseli.thoucylinder.compose.track.TrackInfoDialog
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.views.AlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Playlist
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AppViewModel
import java.util.UUID

@Composable
fun AppCallbacksComposables(
    viewModel: AppViewModel,
    onCancel: () -> Unit,
    onPlaylistClick: (UUID) -> Unit,
    onAlbumClick: (UUID) -> Unit,
    onOpenCreatePlaylistDialog: () -> Unit,
    deleteAlbumCombos: Collection<AbstractAlbumCombo>? = null,
    editTrackCombo: AbstractTrackCombo? = null,
    editAlbum: Album? = null,
    addDownloadedAlbum: Album? = null,
    createPlaylist: Boolean = false,
    addToPlaylist: Boolean = false,
    addToPlaylistSelection: Selection? = null,
    infoTrackCombo: AbstractTrackCombo? = null,
) {
    val context = LocalContext.current
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()

    val displayAddedToPlaylistMessage: (UUID, Int) -> Unit = { playlistId, trackCount ->
        SnackbarEngine.addInfo(
            message = context.resources
                .getQuantityString(R.plurals.x_tracks_added_to_playlist, trackCount, trackCount)
                .umlautify(),
            actionLabel = context.getString(R.string.go_to_playlist).umlautify(),
            onActionPerformed = { onPlaylistClick(playlistId) },
        )
    }

    addDownloadedAlbum?.also { album ->
        if (localMusicUri == null) {
            LocalMusicUriDialog(
                onSave = { viewModel.setLocalMusicUri(it) },
                text = { Text(stringResource(R.string.you_need_to_select_your_local_music_root_folder)) },
            )
        } else {
            onCancel()
            viewModel.downloadAlbum(
                albumId = album.albumId,
                context = context,
                onFinish = { hasErrors ->
                    SnackbarEngine.addInfo(
                        message = if (hasErrors)
                            context.getString(R.string.album_was_downloaded_with_errors, album).umlautify()
                        else context.getString(R.string.album_was_downloaded, album).umlautify(),
                        actionLabel = context.getString(R.string.go_to_album).umlautify(),
                        onActionPerformed = { onAlbumClick(album.albumId) },
                    )
                },
                onTrackError = { track, throwable ->
                    SnackbarEngine.addError("Error on downloading $track: $throwable")
                },
            )
        }
    }

    if (addToPlaylist) {
        addToPlaylistSelection?.also { selection ->
            val playlists by viewModel.playlists.collectAsStateWithLifecycle(emptyList())
            val scope = rememberCoroutineScope()
            var playlistId by rememberSaveable { mutableStateOf<UUID?>(null) }
            var duplicateCount by rememberSaveable { mutableIntStateOf(0) }

            if (duplicateCount > 0 && playlistId != null) {
                AddDuplicatesToPlaylistDialog(
                    duplicateCount = duplicateCount,
                    onAddDuplicatesClick = {
                        onCancel()
                        playlistId?.also {
                            viewModel.addSelectionToPlaylist(selection, it, true) { added ->
                                displayAddedToPlaylistMessage(it, added)
                            }
                        }
                    },
                    onSkipDuplicatesCount = {
                        onCancel()
                        playlistId?.also {
                            viewModel.addSelectionToPlaylist(selection, it, false) { added ->
                                displayAddedToPlaylistMessage(it, added)
                            }
                        }
                    },
                    onCancel = onCancel,
                )
            } else {
                AddToPlaylistDialog(
                    playlists = playlists,
                    onSelect = { pojo ->
                        playlistId = pojo.playlistId
                        scope.launch {
                            duplicateCount = viewModel.getDuplicatePlaylistTrackCount(pojo.playlistId, selection)

                            if (duplicateCount == 0) {
                                onCancel()
                                viewModel.addSelectionToPlaylist(selection, pojo.playlistId) { added ->
                                    displayAddedToPlaylistMessage(pojo.playlistId, added)
                                }
                            }
                        }
                    },
                    onCancel = onCancel,
                    onCreateNewClick = onOpenCreatePlaylistDialog,
                )
            }
        }
    }

    if (createPlaylist) {
        val scope = rememberCoroutineScope()

        CreatePlaylistDialog(
            onSave = { name ->
                val playlist = Playlist(name = name)

                viewModel.createPlaylist(playlist, addToPlaylistSelection)

                if (addToPlaylistSelection != null) scope.launch(Dispatchers.IO) {
                    displayAddedToPlaylistMessage(
                        playlist.playlistId,
                        viewModel.listSelectionTracks(addToPlaylistSelection).size,
                    )
                }
                else onPlaylistClick(playlist.playlistId)

                onCancel()
            },
            onCancel = onCancel,
        )
    }

    deleteAlbumCombos?.also { combos ->
        val albumIds = combos.map { it.album.albumId }

        if (combos.all { !it.album.isLocal && !it.isPartiallyDownloaded }) {
            viewModel.hideAlbums(albumIds) {
                SnackbarEngine.addInfo(
                    message = context.resources.getQuantityString(
                        R.plurals.removed_x_albums_from_library,
                        combos.size,
                        combos.size,
                    ).umlautify(),
                    actionLabel = context.getString(R.string.undo).umlautify(),
                    onActionPerformed = { viewModel.unhideAlbums(albumIds) },
                )
            }
            onCancel()
        } else {
            DeleteAlbumsDialog(
                count = combos.size,
                onCancel = onCancel,
                onDeleteAlbumsClick = {
                    viewModel.hideAlbums(albumIds) {
                        SnackbarEngine.addInfo(
                            message = context.resources.getQuantityString(
                                R.plurals.removed_x_albums_from_library,
                                albumIds.size,
                                albumIds.size,
                            ).umlautify(),
                            actionLabel = context.getString(R.string.undo).umlautify(),
                            onActionPerformed = { viewModel.unhideAlbums(albumIds) },
                        )
                    }
                    onCancel()
                },
                onDeleteAlbumsAndFilesClick = {
                    viewModel.hideAlbumsAndDeleteFiles(albumIds) {
                        SnackbarEngine.addInfo(
                            message = context.resources.getQuantityString(
                                R.plurals.removed_x_albums_and_local_files,
                                albumIds.size,
                                albumIds.size,
                            ).umlautify(),
                            actionLabel = context.getString(R.string.undelete_album).umlautify(),
                            onActionPerformed = { viewModel.unhideAlbums(albumIds) },
                        )
                    }
                    onCancel()
                },
                onDeleteFilesClick = {
                    viewModel.deleteLocalAlbumFiles(albumIds) {
                        SnackbarEngine.addInfo(
                            context.resources.getQuantityString(
                                R.plurals.deleted_local_album_files,
                                albumIds.size,
                                albumIds.size,
                            ).umlautify(),
                        )
                    }
                    onCancel()
                },
            )
        }
    }

    editAlbum?.also { album ->
        EditAlbumMethodDialog(albumId = album.albumId, onClose = onCancel)
    }

    editTrackCombo?.also { combo ->
        EditTrackDialog(trackCombo = combo, onClose = onCancel)
    }

    infoTrackCombo?.also { combo ->
        var albumCombo by rememberSaveable { mutableStateOf<AlbumCombo?>(null) }
        var localPath by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            viewModel.ensureTrackMetadata(combo.track)
            combo.track.albumId?.also { albumCombo = viewModel.getAlbumCombo(it) }
            localPath = combo.track.getLocalAbsolutePath(context)
        }

        TrackInfoDialog(
            track = combo.track,
            albumTitle = albumCombo?.album?.title,
            albumArtist = albumCombo?.artists?.joined(),
            year = combo.track.year ?: albumCombo?.album?.year,
            localPath = localPath,
            onClose = onCancel,
        )
    }
}
