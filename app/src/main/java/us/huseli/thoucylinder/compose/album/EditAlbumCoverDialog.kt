package us.huseli.thoucylinder.compose.album

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel
import java.util.UUID

@Composable
fun EditAlbumCoverDialog(
    albumId: UUID,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val albumArts by viewModel.flowAlbumArt2(albumId, context).collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAlbumArt.collectAsStateWithLifecycle()
    val selectFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.saveAlbumArtFromUri(
                albumId = albumId,
                uri = uri,
                context = context,
                onSuccess = {
                    onClose()
                    SnackbarEngine.addInfo(context.getString(R.string.updated_album_cover))
                },
                onFail = { SnackbarEngine.addError(context.getString(R.string.could_not_open_the_selected_image)) },
            )
        }
    }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.select_album_cover)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(
                onClick = { selectFileLauncher.launch(arrayOf("image/*")) },
                content = { Text(stringResource(R.string.local_file)) },
            )
            TextButton(
                onClick = {
                    viewModel.saveAlbumArt(albumId, null, context)
                    onClose()
                },
                content = { Text(stringResource(R.string.clear)) },
            )
            TextButton(
                onClick = onClose,
                content = { Text(text = stringResource(R.string.cancel)) },
            )
        },
        confirmButton = {},
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(albumArts) { albumArt ->
                    Column(
                        modifier = Modifier.fillMaxSize().clickable {
                            viewModel.saveAlbumArt(albumId, albumArt, context)
                            onClose()
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Thumbnail(
                            image = albumArt.imageBitmap,
                            shape = MaterialTheme.shapes.extraSmall,
                            borderWidth = if (albumArt.isCurrent) 4.dp else 1.dp,
                            borderColor = if (albumArt.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(text = "${albumArt.imageBitmap.width}x${albumArt.imageBitmap.height}")
                    }
                }
                if (isLoading) {
                    item {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxSize().aspectRatio(1f),
                            shape = MaterialTheme.shapes.extraSmall,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(10.dp),
                            content = { Text(stringResource(R.string.loading)) },
                        )
                    }
                }
            }
        },
    )
}
