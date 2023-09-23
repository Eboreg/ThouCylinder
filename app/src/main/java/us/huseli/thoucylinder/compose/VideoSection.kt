package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.YoutubeVideo
import us.huseli.thoucylinder.formattedString
import us.huseli.thoucylinder.viewmodels.YoutubeVideoViewModel

@Composable
fun VideoSection(video: YoutubeVideo, modifier: Modifier = Modifier, position: Int? = null) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val factory = (owner as? NavBackStackEntry)?.let {
        HiltViewModelFactory(context = LocalContext.current, navBackStackEntry = it)
    }
    val viewModel = viewModel<YoutubeVideoViewModel>(owner, factory = factory, key = video.id)

    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val isDownloaded by viewModel.isDownloaded.collectAsStateWithLifecycle(initialValue = false)
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle(initialValue = false)
    val metadata by viewModel.metadata.collectAsStateWithLifecycle(initialValue = null)

    viewModel.setVideo(video)

    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
        Column(modifier = modifier.padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = position?.let { "$position. ${video.title}" } ?: video.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    metadata?.let {
                        val sampleRate = (it.sampleRate.toDouble() / 1000).formattedString(1)
                        val bitrate = it.bitrate / 1000
                        Text(text = "${it.type} / $sampleRate KHz / $bitrate Kbps")
                    }
                }
                video.duration?.let { Text(text = it.sensibleFormat()) }
                IconButton(
                    onClick = { viewModel.playOrPause() },
                    content = {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Sharp.Pause,
                                contentDescription = stringResource(R.string.pause),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Sharp.PlayArrow,
                                contentDescription = stringResource(R.string.play),
                            )
                        }
                    }
                )
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

            downloadProgress?.let { progress ->
                val statusText = stringResource(progress.status.stringId)

                Column(modifier = Modifier.padding(bottom = 5.dp)) {
                    Text(text = "$statusText ...")
                    LinearProgressIndicator(
                        progress = progress.progress.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
