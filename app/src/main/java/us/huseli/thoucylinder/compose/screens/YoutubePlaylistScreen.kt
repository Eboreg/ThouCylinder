package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.AlbumArt
import us.huseli.thoucylinder.compose.EditAlbumDialog
import us.huseli.thoucylinder.compose.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.VideoSection
import us.huseli.thoucylinder.viewmodels.YoutubePlaylistViewModel

@Composable
fun YoutubePlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubePlaylistViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
) {
    val downloadedAlbum by viewModel.downloadedAlbum.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val isDownloaded by viewModel.isDownloaded.collectAsStateWithLifecycle(initialValue = false)
    val playlistNullable by viewModel.playlist.collectAsStateWithLifecycle()
    val thumbnail by viewModel.albumArt.collectAsStateWithLifecycle()
    val thumbnailLoadStatus by viewModel.albumArtLoadStatus.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val videosLoadStatus by viewModel.videosLoadStatus.collectAsStateWithLifecycle()

    downloadedAlbum?.let { album ->
        EditAlbumDialog(
            album = album,
            onSave = { viewModel.saveDownloadedAlbum(it) },
            onCancel = { viewModel.deleteDownloadedAlbum() },
        )
    }

    LazyColumn(modifier = modifier) {
        item {
            AlbumArt(image = thumbnail, loadStatus = thumbnailLoadStatus, modifier = Modifier.fillMaxHeight()) {
                if (onBackClick != null) {
                    FilledTonalIconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Sharp.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                }
            }
        }

        playlistNullable?.let { playlist ->
            item {
                Row(
                    modifier = Modifier
                        .padding(start = 10.dp, end = if (!isDownloaded) 0.dp else 10.dp)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = playlist.toString(), modifier = Modifier.weight(1f))
                    if (!isDownloaded) {
                        IconButton(
                            onClick = { viewModel.download() },
                            content = {
                                Icon(
                                    imageVector = Icons.Sharp.Download,
                                    contentDescription = stringResource(R.string.download),
                                )
                            },
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
                        text = "$statusText ${progress.item} ...",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = progress.progress.toFloat(),
                    )
                }
            }
        }

        if (videosLoadStatus == LoadStatus.LOADING) {
            item {
                ObnoxiousProgressIndicator(text = stringResource(R.string.loading_videos_scream))
            }
        }

        items(videos) { video ->
            VideoSection(video = video.video, position = video.position + 1)
        }
    }
}
