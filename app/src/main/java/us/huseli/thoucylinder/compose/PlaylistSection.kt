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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import us.huseli.thoucylinder.LoadStatus
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.repositories.YoutubeRepository
import us.huseli.thoucylinder.viewmodels.PlaylistViewModel

@Composable
fun PlaylistSection(playlist: YoutubePlaylist, modifier: Modifier = Modifier) {
    val viewModel = ViewModelProvider(
        owner = checkNotNull(LocalViewModelStoreOwner.current),
    )[playlist.id, PlaylistViewModel::class.java]
    val context = LocalContext.current
    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier, shape = ShapeDefaults.ExtraSmall) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playlist.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded },
                )
                IconButton(
                    onClick = { viewModel.download(playlist, context) },
                    content = { Icon(imageVector = Icons.Sharp.Download, contentDescription = "Download") },
                )
            }

            if (downloadStatus.status != YoutubeRepository.DownloadStatus.Status.IDLE) {
                val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

                @Suppress("KotlinConstantConditions")
                val statusText = when (downloadStatus.status) {
                    YoutubeRepository.DownloadStatus.Status.IDLE -> ""
                    YoutubeRepository.DownloadStatus.Status.DOWNLOADING -> "Downloading"
                    YoutubeRepository.DownloadStatus.Status.CONVERTING -> "Converting"
                    YoutubeRepository.DownloadStatus.Status.MOVING -> "Moving"
                }

                Text(
                    text = "$statusText ${downloadStatus.video?.title ?: ""} ...",
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    progress = downloadProgress.toFloat(),
                )
            }

            if (isExpanded) {
                val thumbnail by viewModel.thumbnail.collectAsStateWithLifecycle()
                val thumbnailLoadStatus by viewModel.thumbnailLoadStatus.collectAsStateWithLifecycle()
                val videos by viewModel.videos.collectAsStateWithLifecycle()
                val videosLoadStatus by viewModel.videosLoadStatus.collectAsStateWithLifecycle()

                viewModel.loadThumbnail(playlist, context)
                viewModel.loadVideos(playlist)

                if (thumbnailLoadStatus == LoadStatus.LOADING) {
                    ObnoxiousProgressIndicator(text = "LOADING ALBUM ART!!!")
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
                    ObnoxiousProgressIndicator(text = "LOADING VIDEOS!!!")
                }
                videos.forEach { video ->
                    VideoSection(video = video.video, position = video.position + 1)
                }
            }
        }
    }
}
