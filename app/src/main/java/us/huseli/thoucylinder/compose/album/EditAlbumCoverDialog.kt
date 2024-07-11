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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.compose.utils.WarningButton
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel

@Composable
fun EditAlbumCoverDialog(
    albumId: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
) {
    val albumArts by viewModel.flowAlbumArt(albumId).collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAlbumArt.collectAsStateWithLifecycle()
    val close: () -> Unit = remember {
        {
            viewModel.cancelAlbumArtFetch(albumId)
            onClose()
        }
    }
    val selectFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.saveAlbumArtFromUri(albumId = albumId, uri = uri, onSuccess = close)
    }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.select_album_cover)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = close,
        dismissButton = {
            CancelButton(onClick = close)
            WarningButton(text = stringResource(R.string.clear)) {
                viewModel.clearAlbumArt(albumId)
                close()
            }
            SaveButton(
                onClick = { selectFileLauncher.launch(arrayOf("image/*")) },
                text = stringResource(R.string.local_file),
            )
        },
        confirmButton = {},
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(albumArts.toList(), key = { it.mediaStoreImage.fullUriString }) { albumArt ->
                    Column(
                        modifier = Modifier.fillMaxSize().clickable {
                            viewModel.saveAlbumArt(albumId, albumArt.mediaStoreImage)
                            close()
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Thumbnail(
                            model = albumArt.bitmap,
                            borderWidth = if (albumArt.isCurrent) 4.dp else 1.dp,
                            borderColor = if (albumArt.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(text = "${albumArt.bitmap.width}x${albumArt.bitmap.height}")
                    }
                }
                if (isLoading) {
                    item {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxSize().aspectRatio(1f),
                            shape = MaterialTheme.shapes.small,
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
