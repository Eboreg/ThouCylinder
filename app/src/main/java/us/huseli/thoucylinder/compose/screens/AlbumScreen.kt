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
import androidx.compose.material.icons.sharp.Bookmark
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Cancel
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import us.huseli.thoucylinder.compose.EditAlbumDialog
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.RoundedIconBlock
import us.huseli.thoucylinder.compose.TrackSection
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumArtLoadStatus by viewModel.albumArtLoadStatus.collectAsStateWithLifecycle()
    val albumNullable by viewModel.album.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val playerPlayingUri by viewModel.playerPlayingUri.collectAsStateWithLifecycle(initialValue = null)
    val trackDownloadProgress by viewModel.trackDownloadProgress.collectAsStateWithLifecycle()

    var addDownloadedAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }

    albumNullable?.let { album ->
        if (addDownloadedAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbum = album,
                onCancel = { addDownloadedAlbumDialogOpen = false },
                onSave = {
                    addDownloadedAlbumDialogOpen = false
                    viewModel.downloadAndSaveAlbum(it)
                },
            )
        } else if (addAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbum = album,
                onCancel = { addAlbumDialogOpen = false },
                onSave = {
                    addAlbumDialogOpen = false
                    viewModel.addToLibrary(it)
                }
            )
        } else if (editAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbum = album,
                onCancel = { editAlbumDialogOpen = false },
                onSave = {
                    editAlbumDialogOpen = false
                    viewModel.update(it)
                }
            )
        }
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
                        modifier = Modifier.fillMaxWidth().padding(5.dp),
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
                    Column(modifier = Modifier.fillMaxWidth().padding(5.dp)) {
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

                    if (!album.isLocal) {
                        if (downloadProgress != null) {
                            IconButton(
                                onClick = { viewModel.cancelDownload() },
                                content = { Icon(Icons.Sharp.Cancel, stringResource(R.string.cancel_download)) },
                            )
                        } else {
                            if (album.isInLibrary) {
                                IconButton(
                                    onClick = {
                                        viewModel.removeFromLibrary()
                                        onBackClick()
                                    },
                                    content = {
                                        Icon(Icons.Sharp.Bookmark, stringResource(R.string.remove_from_library))
                                    },
                                )
                            } else {
                                IconButton(
                                    onClick = { addAlbumDialogOpen = true },
                                    content = {
                                        Icon(Icons.Sharp.BookmarkBorder, stringResource(R.string.add_to_library))
                                    },
                                )
                            }
                            IconButton(
                                onClick = { addDownloadedAlbumDialogOpen = true },
                                content = { Icon(Icons.Sharp.Download, stringResource(R.string.download)) },
                            )
                        }
                    }
                    if (album.isInLibrary) {
                        IconButton(
                            onClick = { editAlbumDialogOpen = true },
                            content = { Icon(Icons.Sharp.Edit, stringResource(R.string.edit)) },
                        )
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
                    playerPlayingUri = playerPlayingUri,
                    downloadProgress = trackDownloadProgress[track.id],
                    onDownloadClick = { viewModel.downloadTrack(track) },
                    onPlayOrPauseClick = { viewModel.playOrPause(track) },
                    showArtist = track.artist != album.artist,
                    onArtistClick = onArtistClick,
                )
            }
        }
    }
}
