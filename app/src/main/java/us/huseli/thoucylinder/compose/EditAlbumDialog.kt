package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.DiscogsSearchResultItem
import us.huseli.thoucylinder.dataclasses.TempAlbum
import us.huseli.thoucylinder.dataclasses.TempTrack
import us.huseli.thoucylinder.viewmodels.DiscogsViewModel

@Composable
fun EditAlbumDialog(
    album: TempAlbum,
    modifier: Modifier = Modifier,
    viewModel: DiscogsViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    onSave: (TempAlbum) -> Unit,
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(emptyList())

    var title by rememberSaveable(album.title) { mutableStateOf(album.title) }
    var artist by rememberSaveable(album.artist) { mutableStateOf(album.artist) }
    var tracks by rememberSaveable(album.tracks) { mutableStateOf(album.tracks) }
    var selectedMasterId by rememberSaveable { mutableStateOf<Int?>(null) }

    viewModel.setTempAlbum(album)

    LaunchedEffect(selectedMasterId) {
        viewModel.getMaster(selectedMasterId).filterNotNull().distinctUntilChanged().collect { master ->
            title = master.title
            artist = master.artist
            tracks = tracks.toMutableList().apply {
                zip(master.tracklist).forEachIndexed { index, (tempTrack, masterTrack) ->
                    set(index, tempTrack.copy(title = masterTrack.title, artist = masterTrack.artist))
                }
            }
        }
    }

    AlertDialog(
        modifier = modifier.padding(10.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = ShapeDefaults.ExtraSmall,
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                onClick = { onSave(album.copy(title = title, artist = artist, tracks = tracks)) },
                content = { Text(text = stringResource(R.string.save)) }
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
                    selectedItemId = selectedMasterId,
                    onSelect = { selectedMasterId = it.id },
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = artist ?: "",
                    onValueChange = { artist = it },
                    label = { OutlinedTextFieldLabel(text = stringResource(R.string.album_artist)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                tracks.forEachIndexed { index, track ->
                    EditAlbumTrackSection(
                        track = track,
                        onArtistChange = {
                            tracks = tracks.toMutableList().apply {
                                this[index] = track.copy(artist = it)
                            }
                        },
                        onTitleChange = {
                            tracks = tracks.toMutableList().apply {
                                this[index] = track.copy(title = it)
                            }
                        },
                    )
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscogsMasterDropdown(
    items: List<DiscogsSearchResultItem>,
    modifier: Modifier = Modifier,
    selectedItemId: Int? = null,
    onSelect: (DiscogsSearchResultItem) -> Unit,
) {
    val selectedItem = items.find { it.id == selectedItemId }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            value = selectedItem?.toString() ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.select_discogs_com_master)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.exposedDropdownSize(true),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item.toString()) },
                    onClick = {
                        onSelect(item)
                        isExpanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}


@Composable
fun EditAlbumTrackSection(
    track: TempTrack,
    modifier: Modifier = Modifier,
    onArtistChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
) {
    var title by rememberSaveable(track.title) { mutableStateOf(track.title) }
    var artist by rememberSaveable(track.artist) { mutableStateOf(track.artist) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.track_number, track.albumPosition!! + 1),
            style = MaterialTheme.typography.labelMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = track.artist ?: "",
                onValueChange = {
                    artist = it
                    onArtistChange(it)
                },
                label = { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) },
                singleLine = true,
                modifier = Modifier.weight(0.5f).height(60.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { OutlinedTextFieldLabel(text = stringResource(R.string.artist)) },
            )
            OutlinedTextField(
                value = track.title,
                onValueChange = {
                    title = it
                    onTitleChange(it)
                },
                label = { OutlinedTextFieldLabel(text = stringResource(R.string.title)) },
                singleLine = true,
                modifier = Modifier.weight(0.5f).height(60.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { OutlinedTextFieldLabel(text = stringResource(R.string.title)) },
            )
        }
    }
}
