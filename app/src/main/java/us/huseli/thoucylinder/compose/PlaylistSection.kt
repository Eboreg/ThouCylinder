package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import us.huseli.thoucylinder.DownloadStatus
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.YoutubePlaylist
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel

@Composable
fun PlaylistSection(playlist: YoutubePlaylist, modifier: Modifier = Modifier) {
    val viewModel = ViewModelProvider(
        owner = checkNotNull(LocalViewModelStoreOwner.current),
    )[playlist.id, PlaylistViewModel::class.java]
    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    val isDownloaded by viewModel.isDownloaded.collectAsStateWithLifecycle(initialValue = false)
    val showVideos by viewModel.showVideos.collectAsStateWithLifecycle()

    viewModel.setPlaylist(playlist)

    Card(modifier = modifier, shape = ShapeDefaults.ExtraSmall) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playlist.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.toggleShowVideos() },
                )
                if (!isDownloaded) {
                    IconButton(
                        onClick = { viewModel.download(playlist) },
                        content = {
                            Icon(
                                imageVector = Icons.Sharp.Download,
                                contentDescription = stringResource(R.string.download),
                            )
                        },
                    )
                }
            }

            if (downloadStatus.status != DownloadStatus.Status.IDLE) {
                val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

                @Suppress("KotlinConstantConditions")
                val statusText = when (downloadStatus.status) {
                    DownloadStatus.Status.IDLE -> ""
                    DownloadStatus.Status.DOWNLOADING -> stringResource(R.string.downloading)
                    DownloadStatus.Status.CONVERTING -> stringResource(R.string.converting)
                    DownloadStatus.Status.MOVING -> stringResource(R.string.moving)
                }

                Text(
                    text = "$statusText ${downloadStatus.item ?: ""} ...",
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    progress = downloadProgress.toFloat(),
                )
            }

            if (showVideos) {
                val thumbnail by viewModel.albumArt.collectAsStateWithLifecycle()
                val thumbnailLoadStatus by viewModel.albumArtLoadStatus.collectAsStateWithLifecycle()
                val videos by viewModel.videos.collectAsStateWithLifecycle()
                val videosLoadStatus by viewModel.videosLoadStatus.collectAsStateWithLifecycle()

                if (thumbnailLoadStatus == LoadStatus.LOADING) {
                    ObnoxiousProgressIndicator(text = stringResource(R.string.loading_album_art_scream))
                }
                thumbnail?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(1f),
                    )
                }

                if (videosLoadStatus == LoadStatus.LOADING) {
                    ObnoxiousProgressIndicator(text = stringResource(R.string.loading_videos_scream))
                }
                videos.forEach { video ->
                    VideoSection(video = video.video, position = video.position + 1)
                }
            }
        }
    }
}
