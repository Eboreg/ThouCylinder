package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.LibraryAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.DeleteAlbumDialog
import us.huseli.thoucylinder.compose.AddAlbumDialog
import us.huseli.thoucylinder.compose.RoundedIconBlock
import us.huseli.thoucylinder.compose.TrackSection
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumArtLoadStatus by viewModel.albumArtLoadStatus.collectAsStateWithLifecycle()
    val albumNullable by viewModel.album.collectAsStateWithLifecycle()
    val downloadedAlbum by viewModel.downloadedAlbum.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val playingUri by viewModel.playingUri.collectAsStateWithLifecycle(initialValue = null)
    val trackDownloadProgress by viewModel.trackDownloadProgress.collectAsStateWithLifecycle()

    var deleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addDownloadedAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(downloadedAlbum) {
        if (downloadedAlbum != null) addDownloadedAlbumDialogOpen = true
    }

    downloadedAlbum?.let { album ->
        if (addDownloadedAlbumDialogOpen) {
            AddAlbumDialog(
                initialAlbum = album,
                onSave = {
                    addDownloadedAlbumDialogOpen = false
                    viewModel.saveDownloadedAlbum(it)
                },
                onCancel = {
                    addDownloadedAlbumDialogOpen = false
                    viewModel.deleteDownloadedAlbum()
                },
            )
        }
    }

    if (addDownloadedAlbumDialogOpen) {
        albumNullable?.let { album ->
            AddAlbumDialog(
                initialAlbum = album,
                onSave = {
                    addDownloadedAlbumDialogOpen = false
                    viewModel.downloadAndAddToLibrary(it)
                },
                onCancel = { addDownloadedAlbumDialogOpen = false },
            )
        }
    }

    if (addAlbumDialogOpen) {
        albumNullable?.let { album ->
            AddAlbumDialog(
                initialAlbum = album,
                onCancel = { addAlbumDialogOpen = false },
                onSave = {
                    addAlbumDialogOpen = false
                    viewModel.addToLibrary(it)
                }
            )
        }
    }

    if (deleteDialogOpen) {
        DeleteAlbumDialog(
            onCancel = { deleteDialogOpen = false },
            onConfirm = { removeFromLibrary, deleteFiles ->
                deleteDialogOpen = false
                if (deleteFiles) viewModel.deleteLocalFiles()
                if (removeFromLibrary) {
                    viewModel.removeFromLibrary()
                    onBackClick()
                }
            }
        )
    }

    LazyColumn(modifier = modifier) {
        item {
            AlbumArt(
                image = albumArt,
                loadStatus = albumArtLoadStatus,
                modifier = Modifier.fillMaxHeight(),
                topContent = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FilledTonalIconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Sharp.ArrowBack, stringResource(R.string.go_back))
                        }
                        Row(modifier = Modifier.padding(5.dp)) {
                            if (albumNullable?.isOnYoutube == true || albumNullable?.isLocal == true) {
                                RoundedIconBlock {
                                    if (albumNullable?.isOnYoutube == true) {
                                        Icon(
                                            painterResource(R.drawable.youtube),
                                            stringResource(R.string.youtube_playlist),
                                            modifier = Modifier.fillMaxHeight(),
                                        )
                                    }
                                    if (albumNullable?.isLocal == true) {
                                        Icon(
                                            painterResource(R.drawable.hard_drive),
                                            stringResource(R.string.stored_locally),
                                            modifier = Modifier.fillMaxHeight(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                bottomContent = {
                    albumNullable?.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                        FlowRow(
                            modifier = Modifier
                                .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                                .align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            genres.forEach { genre ->
                                Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        content = { Text(text = genre) },
                                    )
                                }
                            }
                        }
                    }
                    albumNullable?.styles?.takeIf { it.isNotEmpty() }?.let { styles ->
                        FlowRow(
                            modifier = Modifier
                                .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                                .align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            styles.forEach { style ->
                                Box(modifier = Modifier.padding(horizontal = 2.5.dp)) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        content = { Text(text = style) },
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        albumNullable?.let { album ->
            item {
                Row(
                    modifier = Modifier
                        .padding(start = 10.dp, end = if (album.youtubePlaylist != null) 0.dp else 10.dp)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = album.toString(), modifier = Modifier.weight(1f))
                    if (album.isInLibrary) {
                        IconButton(
                            onClick = {
                                if (album.isLocal) deleteDialogOpen = true
                                else {
                                    viewModel.removeFromLibrary()
                                    onBackClick()
                                }
                            },
                            content = { Icon(Icons.Sharp.Delete, stringResource(R.string.delete_album)) },
                        )
                    } else {
                        IconButton(
                            onClick = { addAlbumDialogOpen = true },
                            content = { Icon(Icons.Sharp.LibraryAdd, stringResource(R.string.add_to_library)) },
                        )
                    }

                    if (!album.isLocal) {
                        if (downloadProgress != null) {
                            IconButton(
                                onClick = { viewModel.cancelDownload() },
                                content = { Icon(Icons.Sharp.Cancel, stringResource(R.string.cancel_download)) },
                            )
                        } else {
                            IconButton(
                                onClick = { addDownloadedAlbumDialogOpen = true },
                                content = { Icon(Icons.Sharp.Download, stringResource(R.string.download)) },
                            )
                        }
                    }
                }
            }

            downloadProgress?.let { progress ->
                item {
                    val statusText = stringResource(progress.status.stringId)

                    Column(modifier = Modifier.padding(10.dp)) {
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

            items(album.tracks) { track ->
                viewModel.loadTrackMetadata(track)

                TrackSection(
                    track = track,
                    playingUri = playingUri,
                    downloadProgress = trackDownloadProgress[track.id],
                    onDownloadClick = { viewModel.downloadTrack(track) },
                    onPlayOrPauseClick = { viewModel.playOrPause(track) },
                )
            }
        }
    }
}
