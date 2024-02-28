package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AbstractImportViewModel
import java.util.UUID

@Composable
fun ImportMethodDialog(
    title: String,
    text: @Composable () -> Unit,
    viewModel: AbstractImportViewModel<*>,
    onDismissRequest: () -> Unit,
    onGotoLibraryClick: () -> Unit,
    onGotoAlbumClick: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val importSelectedAlbums: (Boolean) -> Unit = { matchYoutube ->
        viewModel.importSelectedAlbums(matchYoutube, context) { importedIds, notFoundCount ->
            val strings = mutableListOf<String>()
            if (importedIds.isNotEmpty()) {
                strings.add(
                    context.resources.getQuantityString(
                        R.plurals.x_albums_imported,
                        importedIds.size,
                        importedIds.size,
                    )
                )
            }
            if (notFoundCount > 0) {
                strings.add(
                    context.resources.getQuantityString(
                        R.plurals.x_albums_not_found,
                        notFoundCount,
                        notFoundCount,
                    )
                )
            }
            if (strings.isNotEmpty()) {
                val actionLabel =
                    if (importedIds.size == 1) context.getString(R.string.go_to_album).umlautify()
                    else context.getString(R.string.go_to_library).umlautify()

                SnackbarEngine.addInfo(
                    message = strings.joinToString(" ").umlautify(),
                    actionLabel = actionLabel,
                    onActionPerformed = {
                        if (importedIds.size == 1) onGotoAlbumClick(importedIds[0])
                        else onGotoLibraryClick()
                    },
                )
            }
        }
        onDismissRequest()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                content = { Text(stringResource(R.string.import_and_match_with_youtube)) },
                onClick = { importSelectedAlbums(true) },
            )
            TextButton(
                content = { Text(stringResource(R.string.import_without_matching)) },
                onClick = { importSelectedAlbums(false) },
            )
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) } },
        title = { Text(title) },
        text = text,
    )
}
