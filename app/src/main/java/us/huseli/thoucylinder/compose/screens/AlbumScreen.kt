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
import androidx.compose.material.icons.sharp.Delete
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
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.EditAlbumDialog
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.utils.RoundedIconBlock
import us.huseli.thoucylinder.compose.AlbumTrackRow
import us.huseli.thoucylinder.compose.SelectedTracksButtons
import us.huseli.thoucylinder.viewmodels.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumArtLoadStatus by viewModel.albumArtLoadStatus.collectAsStateWithLifecycle()
    val albumPojo by viewModel.albumPojo.collectAsStateWithLifecycle(null)
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val playerPlayingTrack by viewModel.playerPlayingTrack.collectAsStateWithLifecycle(null)
    val trackDownloadProgress by viewModel.trackDownloadProgress.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    var addDownloadedAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editAlbumDialogOpen by rememberSaveable { mutableStateOf(false) }

    albumPojo?.let { pojo ->
        if (addDownloadedAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbumPojo = pojo,
                onCancel = { addDownloadedAlbumDialogOpen = false },
                onSave = {
                    addDownloadedAlbumDialogOpen = false
                    viewModel.downloadAndSaveAlbum(it)
                },
            )
        } else if (addAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbumPojo = pojo,
                onCancel = { addAlbumDialogOpen = false },
                onSave = {
                    addAlbumDialogOpen = false
                    viewModel.addToLibrary(it)
                }
            )
        } else if (editAlbumDialogOpen) {
            EditAlbumDialog(
                initialAlbumPojo = pojo,
                onCancel = { editAlbumDialogOpen = false },
                onSave = {
                    editAlbumDialogOpen = false
                    viewModel.updateAlbumWithTracks(it)
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            selection = selection,
            onAddToPlaylistClick = { onAddToPlaylistClick(selection) },
            onUnselectAllClick = { viewModel.unselectAllTracks() },
        )

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
                                if (albumPojo?.album?.isOnYoutube == true || albumPojo?.album?.isLocal == true) {
                                    RoundedIconBlock {
                                        if (albumPojo?.album?.isOnYoutube == true) {
                                            Icon(
                                                painterResource(R.drawable.youtube),
                                                stringResource(R.string.youtube_playlist),
                                                modifier = Modifier.fillMaxHeight(),
                                            )
                                        }
                                        if (albumPojo?.album?.isLocal == true) {
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
                            albumPojo?.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
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
                                                content = { Text(text = genre.genreName) },
                                            )
                                        }
                                    }
                                }
                            }
                            albumPojo?.styles?.takeIf { it.isNotEmpty() }?.let { styles ->
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
                                                content = { Text(text = style.styleName) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            albumPojo?.let { pojo ->
                item {
                    Row(
                        modifier = Modifier
                            .padding(start = 10.dp, end = if (pojo.album.youtubePlaylist != null) 0.dp else 10.dp)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = pojo.toString(), modifier = Modifier.weight(1f))

                        if (!pojo.album.isLocal) {
                            if (downloadProgress != null) {
                                IconButton(
                                    onClick = { viewModel.cancelDownload() },
                                    content = { Icon(Icons.Sharp.Cancel, stringResource(R.string.cancel_download)) },
                                )
                            } else {
                                if (pojo.album.isInLibrary) {
                                    IconButton(
                                        onClick = {
                                            viewModel.removeAlbumFromLibrary()
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
                        if (pojo.album.isInLibrary) {
                            IconButton(
                                onClick = { editAlbumDialogOpen = true },
                                content = { Icon(Icons.Sharp.Edit, stringResource(R.string.edit)) },
                            )
                        }
                        if (BuildConfig.DEBUG) {
                            IconButton(
                                onClick = { viewModel.deleteAlbumWithTracks() },
                                content = { Icon(Icons.Sharp.Delete, null) },
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

                items(pojo.tracks) { track ->
                    viewModel.loadTrackMetadata(track)

                    AlbumTrackRow(
                        track = track,
                        downloadProgress = trackDownloadProgress[track.trackId],
                        onDownloadClick = { viewModel.downloadTrack(track) },
                        onPlayOrPauseClick = { viewModel.playOrPause(track) },
                        showArtist = track.artist != pojo.album.artist,
                        onArtistClick = onArtistClick,
                        onAddToPlaylistClick = { onAddToPlaylistClick(Selection(tracks = listOf(track))) },
                        isPlaying = playerPlayingTrack?.trackId == track.trackId,
                        onToggleSelected = { viewModel.toggleTrackSelected(track) },
                        isSelected = selection.isTrackSelected(track),
                        selectOnShortClick = selection.tracks.isNotEmpty(),
                    )
                }
            }
        }
    }
}
