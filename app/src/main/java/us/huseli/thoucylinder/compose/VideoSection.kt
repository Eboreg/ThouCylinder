package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.formattedString
import us.huseli.thoucylinder.repositories.YoutubeRepository
import us.huseli.thoucylinder.viewmodels.VideoViewModel

@Composable
fun VideoSection(
    video: YoutubeVideo,
    modifier: Modifier = Modifier,
    position: Int? = null,
) {
    val viewModel = ViewModelProvider(
        owner = checkNotNull(LocalViewModelStoreOwner.current),
    )[video.id, VideoViewModel::class.java]
    val context = LocalContext.current

    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val streamDict by viewModel.streamDict.collectAsStateWithLifecycle()

    viewModel.loadStreamDict(video)

    Card(shape = ShapeDefaults.ExtraSmall) {
        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
            Column(modifier = modifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = position?.let { "$position. ${video.title}" } ?: video.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        streamDict?.let {
                            val sampleRate = (it.sampleRate.toDouble() / 1000).formattedString(1)
                            val bitrate = it.bitrate / 1000
                            Text(text = "${it.type} / $sampleRate KHz / $bitrate Kbps")
                        }
                    }
                    video.length?.let { Text(text = it.sensibleFormat()) }
                    IconButton(
                        onClick = { viewModel.play(video, context) },
                        content = {
                            if (isPlaying) Icon(imageVector = Icons.Sharp.Pause, contentDescription = "Pause")
                            else Icon(imageVector = Icons.Sharp.PlayArrow, contentDescription = "Play")
                        }
                    )
                    IconButton(
                        onClick = { viewModel.download(video, context) },
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

                    Column(modifier = Modifier.padding(bottom = 5.dp)) {
                        Text(text = "$statusText ...")
                        LinearProgressIndicator(
                            progress = downloadProgress.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
