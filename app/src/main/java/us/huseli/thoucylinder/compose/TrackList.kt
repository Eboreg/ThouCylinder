package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackList(
    tracks: List<Track>,
    viewModel: BaseViewModel,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    showArtist: (Track) -> Boolean = { true },
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val playerPlayingTrack by viewModel.playerPlayingTrack.collectAsStateWithLifecycle(null)

    ItemList(
        things = tracks,
        selector = { track -> track.title },
    ) { track ->
        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(Unit) {
            track.image?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
        }

        Row {
            AlbumArt(image = imageBitmap.value, modifier = Modifier.fillMaxHeight().padding(end = 10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight().padding(vertical = 5.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = track.toString(showAlbumPosition = false, showArtist = showArtist(track)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                track.metadata?.let {
                    Text(text = it.duration.sensibleFormat(), modifier = Modifier.padding(start = 5.dp))
                }

                TrackContextMenu(
                    track = track,
                    onDownloadClick = { onDownloadClick(track) },
                    modifier = Modifier.padding(start = 10.dp).width(30.dp),
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )

                IconButton(
                    onClick = { onPlayOrPauseClick(track) },
                    content = {
                        if (playerPlayingTrack == track) Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                        else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
                    },
                )
            }
        }

        downloadProgressMap[track.id]?.let { progress ->
            val statusText = stringResource(progress.status.stringId)

            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                Text(text = "$statusText …")
                LinearProgressIndicator(
                    progress = progress.progress.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
