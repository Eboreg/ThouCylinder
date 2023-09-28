package us.huseli.thoucylinder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Album
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.viewmodels.DiscogsViewModel

@Composable
fun AddAlbumDialog(
    initialAlbum: Album,
    modifier: Modifier = Modifier,
    viewModel: DiscogsViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    onSave: (Album) -> Unit,
) {
    val album by viewModel.album.collectAsStateWithLifecycle()
    val imagePairs by viewModel.images.collectAsStateWithLifecycle()
    var step2 by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialAlbum) {
        viewModel.setAlbum(initialAlbum)
    }

    if (!step2) {
        AddAlbumDialogStep1(
            modifier = modifier,
            viewModel = viewModel,
            onConfirm = { step2 = true },
            onCancel = onCancel,
        )
    } else {
        if (imagePairs.size > 1) {
            AddAlbumDialogStep2(
                modifier = modifier,
                imagePairs = imagePairs,
                onSelect = {
                    album?.copy(albumArt = it)?.let(onSave) ?: run(onCancel)
                },
                onCancel = {
                    onCancel()
                },
            )
        } else {
            album?.let(onSave) ?: run(onCancel)
        }
    }
}


@Composable
fun AddAlbumDialogStep1(
    modifier: Modifier = Modifier,
    viewModel: DiscogsViewModel,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current

    val album by viewModel.album.collectAsStateWithLifecycle()
    val initialTracks by viewModel.initialTracks.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val loadingSearchResults by viewModel.loadingSearchResults.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedMasterId by viewModel.selectedMasterId.collectAsStateWithLifecycle()

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(text = stringResource(R.string.add_album_to_library)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = ShapeDefaults.ExtraSmall,
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                content = { Text(text = stringResource(R.string.save)) },
                enabled = !loading,
            )
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                DiscogsMasterDropdown(
                    items = searchResults,
                    isLoading = loadingSearchResults,
                    selectedItemId = selectedMasterId,
                    onSelect = { viewModel.selectMasterId(it.id, context) },
                    onExpandedChange = { expanded ->
                        if (expanded) album?.let { viewModel.loadSearchResults(it) }
                    },
                )

                OutlinedTextField(
                    value = album?.title ?: "",
                    onValueChange = { viewModel.setTitle(it) },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                )
                OutlinedTextField(
                    value = album?.artist ?: "",
                    onValueChange = { viewModel.setArtist(it) },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                )

                album?.tracks?.forEachIndexed { index, track ->
                    EditAlbumTrackSection(
                        track = track,
                        enabled = !loading,
                        initialTrack = initialTracks[index],
                        onChange = { viewModel.updateTrack(index, it) },
                    )
                }
            }
        }
    )
}


@Composable
fun AddAlbumDialogStep2(
    modifier: Modifier = Modifier,
    imagePairs: List<Pair<Image, ImageBitmap>>,
    onSelect: (Image?) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(text = stringResource(R.string.select_cover_art)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = ShapeDefaults.ExtraSmall,
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                onClick = { onSelect(null) },
                content = { Text(text = stringResource(R.string.none_of_them)) },
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(imagePairs) { (image, imageBitmap) ->
                    AlbumArt(image = imageBitmap, modifier = Modifier.clickable { onSelect(image) })
                }
            }
        },
    )
}
