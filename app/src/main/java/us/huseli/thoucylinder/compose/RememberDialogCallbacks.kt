package us.huseli.thoucylinder.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.album.DeleteAlbumsDialog
import us.huseli.thoucylinder.compose.album.EditAlbumMethodDialog
import us.huseli.thoucylinder.compose.export.ExportTracksDialog
import us.huseli.thoucylinder.compose.imports.PostImportDialog
import us.huseli.thoucylinder.compose.playlist.AddTracksToPlaylistDialog
import us.huseli.thoucylinder.compose.playlist.CreatePlaylistDialog
import us.huseli.thoucylinder.compose.settings.LocalMusicUriDialog
import us.huseli.thoucylinder.compose.track.EditTrackDialog
import us.huseli.thoucylinder.compose.track.TrackInfoDialog
import us.huseli.thoucylinder.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.thoucylinder.managers.ExternalContentManager
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.RootStateViewModel

@Composable
inline fun rememberDialogCallbacks(
    crossinline onGotoAlbumClick: @DisallowComposableCalls (String) -> Unit,
    crossinline onGotoPlaylistClick: @DisallowComposableCalls (String) -> Unit,
    viewModel: RootStateViewModel = hiltViewModel(),
): AppDialogCallbacks {
    val addToPlaylistTrackIds by viewModel.addToPlaylistTrackIds.collectAsStateWithLifecycle()
    val albumToDownload by viewModel.albumToDownload.collectAsStateWithLifecycle()
    val deleteAlbumIds by viewModel.deleteAlbums.collectAsStateWithLifecycle()
    val editAlbumId by viewModel.editAlbumId.collectAsStateWithLifecycle()
    val editTrackState by viewModel.editTrackState.collectAsStateWithLifecycle()
    val exportAlbumIds by viewModel.exportAlbumIds.collectAsStateWithLifecycle()
    val exportPlaylistId by viewModel.exportPlaylistId.collectAsStateWithLifecycle()
    val exportTrackIds by viewModel.exportTrackIds.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val showInfoTrackCombo by viewModel.showInfoTrackCombo.collectAsStateWithLifecycle()
    val showLibraryRadioDialog by viewModel.showLibraryRadioDialog.collectAsStateWithLifecycle()
    var albumImportData by remember { mutableStateOf<List<ExternalContentManager.AlbumImportData>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.addExternalContentCallback(
            object : ExternalContentManager.Callback {
                override fun onAlbumImportFinished(data: List<ExternalContentManager.AlbumImportData>) {
                    albumImportData = data
                }
            }
        )
    }

    albumImportData?.also { data ->
        PostImportDialog(data = data, onDismissRequest = { albumImportData = null })
    }

    if (exportTrackIds.isNotEmpty() || exportAlbumIds.isNotEmpty() || exportPlaylistId != null) {
        ExportTracksDialog(
            trackIds = exportTrackIds,
            albumIds = exportAlbumIds,
            playlistId = exportPlaylistId,
            onClose = { viewModel.clearExports() },
        )
    }

    addToPlaylistTrackIds.takeIf { it.isNotEmpty() }?.also { trackIds ->
        AddTracksToPlaylistDialog(
            trackIds = trackIds.toImmutableList(),
            onPlaylistClick = { onGotoPlaylistClick(it) },
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
            viewModel.downloadAlbum(album = album, onGotoAlbumClick = { onGotoAlbumClick(album.albumId) })
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onSave = { name ->
                viewModel.createPlaylist(name = name, onFinish = { onGotoPlaylistClick(it) })
                viewModel.showCreatePlaylistDialog.value = false
            },
            onCancel = { viewModel.showCreatePlaylistDialog.value = false },
        )
    }

    deleteAlbumIds.takeIf { it.isNotEmpty() }?.also { albumIds ->
        DeleteAlbumsDialog(albumIds = albumIds, onClose = { viewModel.deleteAlbums.value = persistentListOf() })
    }

    editAlbumId?.also { EditAlbumMethodDialog(albumId = it, onClose = { viewModel.editAlbumId.value = null }) }

    editTrackState?.also { EditTrackDialog(state = it, onClose = { viewModel.setEditTrackId(null) }) }

    showInfoTrackCombo?.also {
        TrackInfoDialog(trackCombo = it, onClose = { viewModel.setShowInfoTrackId(null) })
    }

    if (showLibraryRadioDialog) RadioDialog(onDismissRequest = { viewModel.showLibraryRadioDialog.value = false })

    return remember {
        AppDialogCallbacks(
            onAddAlbumsToPlaylistClick = { viewModel.setAddToPlaylistAlbumIds(it) },
            onAddArtistToPlaylistClick = { viewModel.setAddToPlaylistArtistId(it) },
            onAddTracksToPlaylistClick = { viewModel.setAddToPlaylistTrackIds(it) },
            onCreatePlaylistClick = { viewModel.showCreatePlaylistDialog.value = true },
            onDeleteAlbumsClick = { viewModel.deleteAlbums.value = it.toImmutableList() },
            onDownloadAlbumClick = { viewModel.setAlbumToDownloadId(it) },
            onEditAlbumClick = { viewModel.editAlbumId.value = it },
            onEditTrackClick = { viewModel.setEditTrackId(it) },
            onExportAlbumsClick = { viewModel.setExportAlbumIds(it) },
            onExportAllTracksClick = { viewModel.setExportAllTracks() },
            onExportPlaylistClick = { viewModel.setExportPlaylistId(it) },
            onExportTracksClick = { viewModel.setExportTrackIds(it) },
            onRadioClick = { viewModel.showLibraryRadioDialog.value = true },
            onShowTrackInfoClick = { viewModel.setShowInfoTrackId(it) },
        )
    }
}
