package us.huseli.thoucylinder.compose

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.dataclasses.Track

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackSection(
    track: Track,
    downloadProgress: DownloadProgress?,
    playingUri: Uri?,
    onDownloadClick: () -> Unit,
    onPlayOrPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }
    var isInfoDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isInfoDialogOpen) {
        TrackInfoDialog(track = track, onClose = { isInfoDialogOpen = false })
    }

    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
        Column(
            modifier = modifier.padding(start = 10.dp).combinedClickable(
                onLongClick = { isMenuShown = true },
                onClick = {},
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = track.toString(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.metadata?.let {
                    Text(text = it.duration.sensibleFormat(), modifier = Modifier.padding(start = 10.dp))
                }
                IconButton(
                    modifier = Modifier.width(30.dp),
                    onClick = { isMenuShown = !isMenuShown },
                    content = {
                        Icon(Icons.Sharp.MoreVert, null)
                        DropdownMenu(
                            expanded = isMenuShown,
                            onDismissRequest = { isMenuShown = false }
                        ) {
                            if (track.isOnYoutube && !track.isDownloaded) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.download)) },
                                    leadingIcon = { Icon(Icons.Sharp.Download, null) },
                                    onClick = {
                                        onDownloadClick()
                                        isMenuShown = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.track_information)) },
                                leadingIcon = { Icon(Icons.Sharp.Info, null) },
                                onClick = {
                                    isInfoDialogOpen = true
                                    isMenuShown = false
                                },
                            )
                        }
                    }
                )
                IconButton(
                    onClick = onPlayOrPauseClick,
                    content = {
                        if (track.playUri != null && playingUri == track.playUri)
                            Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                        else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
                    },
                )
            }

            downloadProgress?.let { progress ->
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
}
