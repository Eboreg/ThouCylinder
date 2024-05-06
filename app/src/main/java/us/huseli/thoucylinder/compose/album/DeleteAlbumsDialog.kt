package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.album.DeleteAlbumsAction.DELETE_ALBUMS_AND_FILES
import us.huseli.thoucylinder.compose.album.DeleteAlbumsAction.DELETE_FILES_KEEP_ALBUM
import us.huseli.thoucylinder.compose.album.DeleteAlbumsAction.HIDE_ALBUMS_KEEP_FILES
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.DeleteAlbumsViewModel

enum class DeleteAlbumsAction { HIDE_ALBUMS_KEEP_FILES, DELETE_ALBUMS_AND_FILES, DELETE_FILES_KEEP_ALBUM }

@Composable
fun DeleteAlbumsDialog(
    albumIds: ImmutableCollection<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAlbumsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val states by viewModel.albumUiStates.collectAsStateWithLifecycle()

    viewModel.setAlbumIds(albumIds)

    if (states.isNotEmpty()) {
        if (states.all { !it.isLocal && !it.isPartiallyDownloaded }) {
            viewModel.removeAlbumsFromLibrary {
                SnackbarEngine.addInfo(
                    message = context.resources.getQuantityString(
                        R.plurals.hid_x_albums_from_library,
                        states.size,
                        states.size,
                        states.first().title,
                    ).umlautify(),
                    actionLabel = context.getString(R.string.undo).umlautify(),
                    onActionPerformed = { viewModel.reAddAlbumsToLibrary() },
                )
            }
            onClose()
        } else {
            DeleteAlbumsDialog(
                count = states.size,
                onCancel = onClose,
                onDeleteClick = { action ->
                    val messageId = when (action) {
                        HIDE_ALBUMS_KEEP_FILES -> R.plurals.hid_x_albums_from_library
                        DELETE_ALBUMS_AND_FILES -> R.plurals.removed_x_albums_and_local_files
                        DELETE_FILES_KEEP_ALBUM -> R.plurals.deleted_local_album_files
                    }
                    val message = context.resources.getQuantityString(
                        messageId,
                        states.size,
                        states.size,
                        states.first().title,
                    ).umlautify()

                    when (action) {
                        HIDE_ALBUMS_KEEP_FILES -> viewModel.hideAlbums {
                            SnackbarEngine.addInfo(
                                message = message,
                                actionLabel = context.getString(R.string.undo).umlautify(),
                                onActionPerformed = { viewModel.unhideAlbums() },
                            )
                        }
                        DELETE_ALBUMS_AND_FILES -> viewModel.hideAlbumsAndDeleteFiles {
                            SnackbarEngine.addInfo(
                                message = message,
                                actionLabel = context.getString(R.string.undelete_album).umlautify(),
                                onActionPerformed = { viewModel.unhideAlbums() },
                            )
                        }
                        DELETE_FILES_KEEP_ALBUM -> viewModel.deleteLocalAlbumFiles {
                            SnackbarEngine.addInfo(message = message)
                        }
                    }
                    onClose()
                },
                modifier = modifier,
            )
        }
    }
}


@Composable
fun DeleteAlbumsDialog(
    count: Int,
    onCancel: () -> Unit,
    onDeleteClick: (DeleteAlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        dismissButton = { CancelButton(onClick = onCancel) },
        title = { Text(pluralStringResource(R.plurals.delete_x_albums, count, count)) },
        confirmButton = {
            SaveButton(
                text = pluralStringResource(R.plurals.remove_album_and_delete_files, count, count),
                onClick = { onDeleteClick(DELETE_ALBUMS_AND_FILES) },
            )
            SaveButton(
                text = pluralStringResource(R.plurals.hide_album_keep_local_files, count, count),
                onClick = { onDeleteClick(HIDE_ALBUMS_KEEP_FILES) },
            )
            SaveButton(
                text = pluralStringResource(R.plurals.delete_local_files_keep_album, count, count),
                onClick = { onDeleteClick(DELETE_FILES_KEEP_ALBUM) },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.what_do_you_want_to_do))
                Text(
                    text = stringResource(R.string.may_not_be_able_to_delete_local_files),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}
