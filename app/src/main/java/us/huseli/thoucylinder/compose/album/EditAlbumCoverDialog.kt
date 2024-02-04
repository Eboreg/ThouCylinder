package us.huseli.thoucylinder.compose.album

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val bitmaps by viewModel.flowAlbumArt(albumId, context).collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingAlbumArt.collectAsStateWithLifecycle()

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(stringResource(R.string.select_album_cover)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onClose,
        dismissButton = {
            TextButton(
                onClick = onClose,
                content = { Text(text = stringResource(R.string.cancel)) },
            )
        },
        confirmButton = {},
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                // contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(bitmaps) { bitmap ->
                    Column(
                        modifier = Modifier.fillMaxSize().clickable {
                            viewModel.saveAlbumArt(albumId, bitmap, context)
                            onClose()
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Thumbnail(image = bitmap, shape = MaterialTheme.shapes.extraSmall)
                        Text(text = "${bitmap.width}x${bitmap.height}")
                    }
                }
                if (isLoading) {
                    item {
                        /*
                        Card(
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.fillMaxSize().aspectRatio(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            content = { Text(stringResource(R.string.loading)) },
                        )
                         */
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
                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveAlbumArt(albumId, null, context)
                            onClose()
                        },
                        modifier = Modifier.fillMaxSize().aspectRatio(1f),
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(10.dp),
                        content = { Text(text = stringResource(R.string.none)) },
                    )

                    /*
                    Card(
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxSize().aspectRatio(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.saveAlbumArt(albumId, null, context)
                                onClose()
                            },
                            content = { Text(text = stringResource(R.string.none)) },
                        )
                    }
                     */
                }
            }
        },
    )
}
