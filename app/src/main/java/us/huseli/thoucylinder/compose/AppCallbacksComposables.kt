package us.huseli.thoucylinder.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.album.DeleteAlbumDialog
import us.huseli.thoucylinder.compose.album.EditAlbumMethodDialog
import us.huseli.thoucylinder.compose.screens.LocalMusicUriDialog
import us.huseli.thoucylinder.compose.track.EditTrackDialog
import us.huseli.thoucylinder.compose.track.TrackInfoDialog
import us.huseli.thoucylinder.dataclasses.Selection
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.viewmodels.AppViewModel
import java.util.UUID

@Composable
fun AppCallbacksComposables(
    viewModel: AppViewModel,
    onCancel: () -> Unit,
    onPlaylistClick: (UUID) -> Unit,
    onAlbumClick: (UUID) -> Unit,
    onOpenCreatePlaylistDialog: () -> Unit,
    deleteAlbumPojo: AbstractAlbumPojo? = null,
    editTrack: Track? = null,
    editAlbum: Album? = null,
    addDownloadedAlbum: Album? = null,
    createPlaylist: Boolean = false,
    addToPlaylist: Boolean = false,
    addToPlaylistSelection: Selection? = null,
    infoTrackPojo: AbstractTrackPojo? = null,
) {
    val context = LocalContext.current
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()

    val displayAddedToPlaylistMessage: (UUID) -> Unit = { playlistId ->
        SnackbarEngine.addInfo(
            message = context.getString(R.string.selection_was_added_to_playlist),
            actionLabel = context.getString(R.string.go_to_playlist),
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
                            context.getString(R.string.album_was_downloaded_with_errors, album)
                        else context.getString(R.string.album_was_downloaded, album),
                        actionLabel = context.getString(R.string.go_to_album),
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

            AddToPlaylistDialog(
                playlists = playlists,
                onSelect = { playlistPojo ->
                    onCancel()
                    viewModel.addSelectionToPlaylist(selection, playlistPojo)
                    displayAddedToPlaylistMessage(playlistPojo.playlistId)
                },
                onCancel = onCancel,
                onCreateNewClick = onOpenCreatePlaylistDialog,
            )
        }
    }

    if (createPlaylist) {
        CreatePlaylistDialog(
            onSave = { name ->
                val playlistPojo = PlaylistPojo(name = name)

                viewModel.createPlaylist(playlistPojo, addToPlaylistSelection)

                if (addToPlaylistSelection != null)
                    displayAddedToPlaylistMessage(playlistPojo.playlistId)
                else onPlaylistClick(playlistPojo.playlistId)

                onCancel()
            },
            onCancel = onCancel,
        )
    }

    deleteAlbumPojo?.also { pojo ->
        if (!pojo.album.isLocal && !pojo.isPartiallyDownloaded) {
            viewModel.markAlbumForDeletion(pojo.album.albumId) {
                SnackbarEngine.addInfo(
                    message = context.getString(R.string.removed_album_from_library, pojo.album.title),
                    actionLabel = context.getString(R.string.undo),
                    onActionPerformed = { viewModel.unmarkAlbumForDeletion(pojo.album.albumId) },
                )
            }
            onCancel()
        } else {
            DeleteAlbumDialog(
                album = pojo.album,
                onCancel = onCancel,
                onDeleteAlbumAndFilesClick = {
                    viewModel.markAlbumForDeletion(pojo.album.albumId) {
                        val message = context.getString(R.string.removed_album_and_local_files, pojo.album.title)

                        if (pojo.album.isOnYoutube) {
                            SnackbarEngine.addInfo(
                                message = message,
                                actionLabel = context.getString(R.string.undelete_album),
                                onActionPerformed = { viewModel.unmarkAlbumForDeletion(pojo.album.albumId) }
                            )
                        } else {
                            SnackbarEngine.addInfo(message)
                        }
                    }
                    onCancel()
                },
                onDeleteFilesClick = {
                    viewModel.deleteLocalAlbumFiles(pojo.album.albumId) {
                        SnackbarEngine.addInfo(context.getString(R.string.deleted_album_local_files, pojo.album.title))
                    }
                    onCancel()
                },
                onDeleteAlbumClick = {
                    viewModel.setAlbumIsHidden(pojo.album.albumId, true) {
                        SnackbarEngine.addInfo(
                            message = context.getString(R.string.removed_from_the_library, pojo.album.title),
                            actionLabel = context.getString(R.string.undo),
                            onActionPerformed = { viewModel.setAlbumIsHidden(pojo.album.albumId, false) },
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

    infoTrackPojo?.also { pojo ->
        var album by rememberSaveable { mutableStateOf(pojo.album) }
        var localPath by rememberSaveable { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            viewModel.ensureTrackMetadata(pojo.track)
            album = pojo.album ?: viewModel.getTrackAlbum(pojo.track.albumId)
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
            onClose = onCancel,
            isOnSpotify = pojo.isOnSpotify,
        )
    }
}