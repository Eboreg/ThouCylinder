package us.huseli.thoucylinder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PauseCircleOutline
import androidx.compose.material.icons.sharp.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackGrid(
    tracks: List<Track>,
    viewModel: BaseViewModel,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val playerCurrentUri by viewModel.playerCurrentUri.collectAsStateWithLifecycle()
    val playerPlaybackState by viewModel.playerPlaybackState.collectAsStateWithLifecycle()
    val overlayIconTint = LocalContentColor.current.copy(alpha = 0.6f)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(tracks) { track ->
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(Unit) {
                track.image?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
            }

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.clickable { onPlayOrPauseClick(track) },
            ) {
                Box(modifier = Modifier.aspectRatio(1f)) {
                    AlbumArt(image = imageBitmap.value, modifier = Modifier.fillMaxWidth())
                    if (playerCurrentUri == track.playUri) {
                        when (playerPlaybackState) {
                            PlayerRepository.PlaybackState.STOPPED -> {}
                            PlayerRepository.PlaybackState.PLAYING -> {
                                Icon(
                                    imageVector = Icons.Sharp.PauseCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    tint = overlayIconTint,
                                )
                            }
                            PlayerRepository.PlaybackState.PAUSED -> {
                                Icon(
                                    imageVector = Icons.Sharp.PlayCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    tint = overlayIconTint,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.padding(5.dp).weight(1f)) {
                        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                            Text(text = track.title, maxLines = 2)
                            Row {
                                track.artist?.let { Text(text = it, modifier = Modifier.weight(1f)) }
                                track.metadata?.duration?.let {
                                    Text(text = it.sensibleFormat(), modifier = Modifier.padding(start = 10.dp))
                                }
                            }
                        }
                    }
                    TrackContextMenu(
                        track = track,
                        onDownloadClick = { onDownloadClick(track) },
                        modifier = Modifier.width(30.dp),
                        onGotoAlbumClick = onGotoAlbumClick,
                        onGotoArtistClick = onGotoArtistClick,
                    )
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
    }
}
