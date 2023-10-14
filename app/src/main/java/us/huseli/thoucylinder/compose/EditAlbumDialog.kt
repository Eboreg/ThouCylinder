package us.huseli.thoucylinder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.OutlinedTextFieldLabel
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.pojos.AlbumWithTracksPojo
import us.huseli.thoucylinder.dataclasses.Image
import us.huseli.thoucylinder.viewmodels.EditAlbumViewModel

@Composable
fun EditAlbumDialog(
    initialAlbumPojo: AlbumWithTracksPojo,
    title: String,
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    onSave: (AlbumWithTracksPojo) -> Unit,
) {
    val pojo by viewModel.albumPojo.collectAsStateWithLifecycle()
    val imagePairs by viewModel.images.collectAsStateWithLifecycle()
    var step2 by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialAlbumPojo) {
        viewModel.setAlbum(initialAlbumPojo)
    }

    if (!step2) {
        EditAlbumDialogStep1(
            modifier = modifier,
            viewModel = viewModel,
            onNextClick = { step2 = true },
            onCancelClick = onCancel,
            title = title,
        )
    } else {
        if (imagePairs.isNotEmpty()) {
            EditAlbumDialogStep2(
                modifier = modifier,
                imagePairs = imagePairs,
                onSelect = { image ->
                    pojo?.let {
                        it.copy(
                            album = it.album.copy(albumArt = image),
                            tracks = it.tracks.map { track -> track.copy(image = image) },
                        )
                    }?.let(onSave) ?: run(onCancel)
                },
                onCancel = onCancel,
                onPreviousClick = { step2 = false },
            )
        } else {
            pojo?.let(onSave) ?: run(onCancel)
        }
    }
}


@Composable
fun EditAlbumDialogStep1(
    modifier: Modifier = Modifier,
    viewModel: EditAlbumViewModel,
    title: String,
    onNextClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val context = LocalContext.current

    val pojo by viewModel.albumPojo.collectAsStateWithLifecycle()
    val initialTracks by viewModel.initialTracks.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val loadingSearchResults by viewModel.loadingSearchResults.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedMasterId by viewModel.selectedMasterId.collectAsStateWithLifecycle()

    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(title) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = ShapeDefaults.ExtraSmall,
        onDismissRequest = onCancelClick,
        confirmButton = {
            TextButton(
                onClick = onNextClick,
                content = { Text(text = stringResource(R.string.next)) },
                enabled = !loading,
            )
        },
        dismissButton = { TextButton(onClick = onCancelClick) { Text(stringResource(R.string.cancel)) } },
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
                        if (expanded) pojo?.album?.let { viewModel.loadSearchResults(it) }
                    },
                )

                OutlinedTextField(
                    value = pojo?.album?.title ?: "",
                    onValueChange = { viewModel.setTitle(it) },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                )
                OutlinedTextField(
                    value = pojo?.album?.artist ?: "",
                    onValueChange = { viewModel.setArtist(it) },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                )

                pojo?.tracks?.forEachIndexed { index, track ->
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
fun EditAlbumDialogStep2(
    modifier: Modifier = Modifier,
    imagePairs: List<Pair<Image, ImageBitmap>>,
    onSelect: (Image?) -> Unit,
    onCancel: () -> Unit,
    onPreviousClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier.padding(10.dp),
        title = { Text(text = stringResource(R.string.select_cover_art)) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = ShapeDefaults.ExtraSmall,
        onDismissRequest = onCancel,
        dismissButton = {
            TextButton(
                onClick = onPreviousClick,
                content = { Text(text = stringResource(R.string.previous)) }
            )
            TextButton(
                onClick = onCancel,
                content = { Text(text = stringResource(R.string.cancel)) },
            )
        },
        confirmButton = {},
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(imagePairs) { (image, imageBitmap) ->
                    Column(
                        modifier = Modifier.clickable { onSelect(image) }.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Thumbnail(image = imageBitmap)
                        Text(text = "${imageBitmap.width}x${imageBitmap.height}")
                    }
                }
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        TextButton(
                            onClick = { onSelect(null) },
                            content = { Text(text = stringResource(R.string.none)) },
                        )
                    }
                }
            }
        },
    )
}
