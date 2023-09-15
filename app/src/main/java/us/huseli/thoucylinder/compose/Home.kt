package us.huseli.thoucylinder.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.data.entities.YoutubePlaylist
import us.huseli.thoucylinder.data.entities.YoutubeVideo
import us.huseli.thoucylinder.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playingVideo by viewModel.playingVideo.collectAsStateWithLifecycle()
    val playlistThumbnails by viewModel.playlistThumbnails.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("frank zappa hot rats") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(text = "Search query") },
        )
        OutlinedButton(
            onClick = { viewModel.search(query) },
            content = { Text("Submit") },
        )
        Text(text = "Playlists", style = MaterialTheme.typography.headlineSmall)
        playlists.forEach { playlist ->
            PlaylistSection(
                playlist = playlist,
                isPlaying = isPlaying,
                playingVideo = playingVideo,
                thumbnail = playlistThumbnails[playlist.id],
                onDownloadClick = { viewModel.downloadTrack(it, context) },
                onPlayOrPauseClick = { viewModel.play(it, context) },
                onExpand = { viewModel.loadPlaylistThumbnail(playlist, context) },
            )
        }
        Text(text = "Videos", style = MaterialTheme.typography.headlineSmall)
        videos.forEach { video ->
            VideoSection(
                video = video,
                isPlaying = isPlaying && playingVideo == video,
                onDownloadClick = { viewModel.downloadTrack(video, context) },
                onPlayOrPauseClick = { viewModel.play(video, context) },
            )
        }
    }
}

@Composable
fun PlaylistSection(
    playlist: YoutubePlaylist,
    playingVideo: YoutubeVideo?,
    isPlaying: Boolean,
    thumbnail: ImageBitmap?,
    onDownloadClick: (YoutubeVideo) -> Unit,
    onPlayOrPauseClick: (YoutubeVideo) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth(), shape = ShapeDefaults.ExtraSmall) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
            Text(
                text = playlist.toString(),
                modifier = Modifier.fillMaxWidth().clickable {
                    isExpanded = !isExpanded
                    if (isExpanded) onExpand()
                },
            )
            if (isExpanded) {
                thumbnail?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(1f),
                    )
                }
                playlist.videos.forEach {
                    VideoSection(
                        video = it,
                        isPlaying = isPlaying && playingVideo == it,
                        onDownloadClick = { onDownloadClick(it) },
                        onPlayOrPauseClick = { onPlayOrPauseClick(it) },
                    )
                }
            }
        }
    }
}

@Composable
fun VideoSection(
    video: YoutubeVideo,
    isPlaying: Boolean,
    onDownloadClick: () -> Unit,
    onPlayOrPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), shape = ShapeDefaults.ExtraSmall) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = video.title, modifier = Modifier.weight(1f))
            video.length?.let { Text(text = it.toString()) }
            IconButton(
                onClick = onPlayOrPauseClick,
                content = {
                    if (isPlaying) Icon(imageVector = Icons.Sharp.Pause, contentDescription = "Pause")
                    else Icon(imageVector = Icons.Sharp.PlayArrow, contentDescription = "Play")
                }
            )
            IconButton(
                onClick = onDownloadClick,
                content = { Icon(imageVector = Icons.Sharp.Download, contentDescription = "Download") },
            )
        }
    }
}
