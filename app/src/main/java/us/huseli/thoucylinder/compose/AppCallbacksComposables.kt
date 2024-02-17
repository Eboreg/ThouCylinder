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
import us.huseli.thoucylinder.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.album.DeleteAlbumDialog
import us.huseli.thoucylinder.compose.album.EditAlbumMethodDialog
import us.huseli.thoucylinder.compose.screens.LocalMusicUriDialog
import us.huseli.thoucylinder.compose.track.EditTrackDialog
import us.huseli.thoucylinder.compose.track.TrackInfoDialog
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.entities.Playlist
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
    deleteAlbumCombo: AbstractAlbumCombo? = null,
    editTrack: Track? = null,
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

    deleteAlbumCombo?.also { combo ->
        if (!combo.album.isLocal && !combo.isPartiallyDownloaded) {
            viewModel.setAlbumIsInLibrary(combo.album.albumId, false) {
                SnackbarEngine.addInfo(
                    message = context.getString(R.string.removed_album_from_library, combo.album.title).umlautify(),
                    actionLabel = context.getString(R.string.undo).umlautify(),
                    onActionPerformed = { viewModel.unmarkAlbumForDeletion(combo.album.albumId) },
                )
            }
            onCancel()
        } else {
            DeleteAlbumDialog(
                album = combo.album,
                onCancel = onCancel,
                onDeleteAlbumAndFilesClick = {
                    viewModel.markAlbumForDeletion(combo.album.albumId) {
                        val message =
                            context.getString(R.string.removed_album_and_local_files, combo.album.title).umlautify()

                        if (combo.album.isOnYoutube) {
                            SnackbarEngine.addInfo(
                                message = message,
                                actionLabel = context.getString(R.string.undelete_album).umlautify(),
                                onActionPerformed = { viewModel.unmarkAlbumForDeletion(combo.album.albumId) }
                            )
                        } else {
                            SnackbarEngine.addInfo(message)
                        }
                    }
                    onCancel()
                },
                onDeleteFilesClick = {
                    viewModel.deleteLocalAlbumFiles(combo.album.albumId) {
                        SnackbarEngine.addInfo(
                            context.getString(R.string.deleted_album_local_files, combo.album.title).umlautify(),
                        )
                    }
                    onCancel()
                },
                onDeleteAlbumClick = {
                    viewModel.setAlbumIsHidden(combo.album.albumId, true) {
                        SnackbarEngine.addInfo(
                            message = context.getString(R.string.removed_from_the_library, combo.album.title)
                                .umlautify(),
                            actionLabel = context.getString(R.string.undo).umlautify(),
                            onActionPerformed = { viewModel.setAlbumIsHidden(combo.album.albumId, false) },
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

    editTrack?.also { track ->
        EditTrackDialog(
            track = track,
            onCancel = onCancel,
            onSave = {
                viewModel.saveTrack(it)
                onCancel()
            },
        )
    }

    infoTrackCombo?.also { combo ->
        var album by rememberSaveable { mutableStateOf(combo.album) }
        var localPath by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            viewModel.ensureTrackMetadata(combo.track)
            album = combo.album ?: viewModel.getTrackAlbum(combo.track.albumId)
            localPath = combo.track.getLocalAbsolutePath(context)
        }

        TrackInfoDialog(
            isDownloaded = combo.track.isDownloaded,
            isOnYoutube = combo.track.isOnYoutube,
            metadata = combo.track.metadata,
            albumTitle = album?.title,
            albumArtist = album?.artist,
            year = combo.track.year ?: album?.year,
            localPath = localPath,
            onClose = onCancel,
            isOnSpotify = combo.track.isOnSpotify,
        )
    }
}
