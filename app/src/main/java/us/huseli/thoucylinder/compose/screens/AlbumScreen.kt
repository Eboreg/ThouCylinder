package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.AlbumBadges
import us.huseli.thoucylinder.compose.AlbumButtons
import us.huseli.thoucylinder.compose.AlbumTrackRow
import us.huseli.thoucylinder.compose.EditAlbumDialog
import us.huseli.thoucylinder.compose.SelectedTracksButtons
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.compose.utils.LargeIconBadge
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumPojo by viewModel.albumPojo.collectAsStateWithLifecycle(null)
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val trackDownloadProgress by viewModel.trackDownloadProgress.collectAsStateWithLifecycle()
    val selectedTracks by viewModel.selectedTracks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var addDownloadedAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }

    albumPojo?.let { pojo ->
        if (addDownloadedAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbumPojo = pojo,
                title = stringResource(R.string.add_album_to_library),
                onCancel = { addDownloadedAlbumDialogOpen = false },
                onSave = {
                    addDownloadedAlbumDialogOpen = false
                    viewModel.downloadAndSaveAlbum(it)
                },
            )
        } else if (addAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbumPojo = pojo,
                title = stringResource(R.string.add_album_to_library),
                onCancel = { addAlbumDialogOpen = false },
                onSave = {
                    addAlbumDialogOpen = false
                    viewModel.saveAlbumWithTracks(it)
                    viewModel.tagAlbumTracks(it)
                }
            )
        } else if (editAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbumPojo = pojo,
                title = stringResource(R.string.update_album),
                onCancel = { editAlbumDialogOpen = false },
                onSave = {
                    editAlbumDialogOpen = false
                    viewModel.saveAlbumWithTracks(it)
                    viewModel.tagAlbumTracks(it)
                }
            )
        }

        Column {
            SelectedTracksButtons(
                trackCount = selectedTracks.size,
                onAddToPlaylistClick = { onAddToPlaylistClick(Selection(tracks = selectedTracks)) },
                onUnselectAllClick = { viewModel.unselectAllTracks() },
            )

            LazyColumn(modifier = modifier.padding(horizontal = 10.dp)) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            content = { Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back)) },
                            modifier = Modifier.width(40.dp),
                        )
                        if (pojo.album.isOnYoutube) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.youtube), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.youtube))
                            }
                        }
                        if (pojo.album.isLocal) {
                            LargeIconBadge {
                                Icon(painterResource(R.drawable.hard_drive), null, modifier = Modifier.height(20.dp))
                                Text(text = stringResource(R.string.local))
                            }
                        }
                    }
                }
                item {
                    var height by remember { mutableStateOf(0.dp) }

                    BoxWithConstraints {
                        height = this.maxWidth * 0.4f

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(0.4f)) {
                                Thumbnail(
                                    image = albumArt,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    placeholder = { Image(Icons.Sharp.Album, null) },
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.6f).heightIn(min = height),
                                verticalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column {
                                    Text(
                                        text = pojo.album.title,
                                        style = if (pojo.album.title.length > 35) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (pojo.album.artist != null) {
                                        Text(
                                            text = pojo.album.artist,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    AlbumBadges(pojo = pojo)
                                }
                                Row {
                                    AlbumButtons(
                                        pojo = pojo,
                                        modifier = Modifier.align(Alignment.Bottom),
                                        onCancelDownloadClick = { viewModel.cancelDownload() },
                                        onRemoveFromLibraryClick = {
                                            viewModel.removeAlbumFromLibrary()
                                            onBackClick()
                                        },
                                        onAddToLibraryClick = { addAlbumDialogOpen = true },
                                        onDownloadClick = { addDownloadedAlbumDialogOpen = true },
                                        onEditClick = { editAlbumDialogOpen = true },
                                        onDeleteClick = { viewModel.deleteAlbumWithTracks() },
                                        onPlayClick = { viewModel.playAlbum() },
                                    )
                                }
                            }
                        }
                    }
                }

                downloadProgress?.let { progress ->
                    item {
                        val statusText = stringResource(progress.status.stringId)

                        Column {
                            Text(
                                text = "$statusText ${progress.item} …",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                progress = progress.progress.toFloat(),
                            )
                        }
                    }
                }

                items(pojo.tracks) { track ->
                    viewModel.loadTrackMetadata(track)

                    AlbumTrackRow(
                        track = track,
                        album = pojo.album,
                        downloadProgress = trackDownloadProgress[track.trackId],
                        onDownloadClick = { viewModel.downloadTrack(track) },
                        onPlayClick = { viewModel.playAlbum(startAt = track) },
                        showArtist = track.artist != pojo.album.artist,
                        onArtistClick = onArtistClick,
                        onAddToPlaylistClick = { onAddToPlaylistClick(Selection(track)) },
                        onToggleSelected = { viewModel.toggleSelected(track) },
                        isSelected = selectedTracks.contains(track),
                        selectOnShortClick = selectedTracks.isNotEmpty(),
                        showDiscNumber = pojo.discCount > 1,
                        onEnqueueNextClick = { viewModel.enqueueTrackNext(track, context) },
                    )
                }
            }
        }
    }
}
