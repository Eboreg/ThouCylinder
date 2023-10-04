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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PauseCircleOutline
import androidx.compose.material.icons.sharp.PlayCircleOutline
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.CoroutineScope
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackGrid(
    tracks: LazyPagingItems<Track>,
    viewModel: BaseViewModel,
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
) {
    TrackGrid(
        viewModel = viewModel,
        onDownloadClick = onDownloadClick,
        onPlayOrPauseClick = onPlayOrPauseClick,
        gridState = gridState,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        trackIterator = { action ->
            items(count = tracks.itemCount, key = tracks.itemKey { it.id }) { index ->
                tracks[index]?.also { track -> action(this, track) }
            }
        },
        onLaunch = onLaunch,
        showArtist = showArtist,
    )
}


@Composable
fun TrackGrid(
    tracks: List<Track>,
    viewModel: BaseViewModel,
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
) {
    TrackGrid(
        viewModel = viewModel,
        onDownloadClick = onDownloadClick,
        onPlayOrPauseClick = onPlayOrPauseClick,
        gridState = gridState,
        onGotoArtistClick = onGotoArtistClick,
        onGotoAlbumClick = onGotoAlbumClick,
        trackIterator = { action ->
            items(tracks) { track -> action(this, track) }
        },
        onLaunch = onLaunch,
        showArtist = showArtist,
    )
}


@Composable
fun TrackGrid(
    viewModel: BaseViewModel,
    gridState: LazyGridState,
    trackIterator: LazyGridScope.(@Composable LazyGridItemScope.(Track) -> Unit) -> Unit,
    showArtist: Boolean,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)?,
    onGotoAlbumClick: ((UUID) -> Unit)?,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)?,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val playerCurrentTrack by viewModel.playerCurrentTrack.collectAsStateWithLifecycle()
    val playerPlaybackState by viewModel.playerPlaybackState.collectAsStateWithLifecycle()
    val overlayIconTint = LocalContentColor.current.copy(alpha = 0.6f)

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        trackIterator { track ->
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
            var isContextMenuShown by rememberSaveable { mutableStateOf(false) }

            if (onLaunch != null) LaunchedEffect(Unit) {
                onLaunch(track)
            }

            LaunchedEffect(Unit) {
                track.image?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
            }

            OutlinedCard(
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.clickable { onPlayOrPauseClick(track) },
            ) {
                Box(modifier = Modifier.aspectRatio(1f)) {
                    AlbumArt(
                        image = imageBitmap.value,
                        modifier = Modifier.fillMaxWidth(),
                        topContent = {
                            Row {
                                Column(modifier = Modifier.weight(1f)) {
                                    track.metadata?.duration?.let { duration ->
                                        Surface(
                                            shape = CutCornerShape(bottomEndPercent = 100),
                                            color = MaterialTheme.colorScheme.error,
                                            contentColor = contentColorFor(MaterialTheme.colorScheme.error),
                                        ) {
                                            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = duration.sensibleFormat(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.rotate(-45f).offset(0.dp, (-10).dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = { isContextMenuShown = !isContextMenuShown },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = contentColorFor(MaterialTheme.colorScheme.error),
                                    ),
                                    modifier = Modifier.size(50.dp),
                                    shape = CutCornerShape(bottomStartPercent = 100),
                                ) {
                                    Icon(
                                        imageVector = Icons.Sharp.MoreVert,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(-45f).offset(10.dp, 0.dp),
                                    )
                                    TrackContextMenu(
                                        track = track,
                                        onDownloadClick = { onDownloadClick(track) },
                                        onDismissRequest = { isContextMenuShown = false },
                                        isShown = isContextMenuShown,
                                        onGotoAlbumClick = onGotoAlbumClick,
                                        onGotoArtistClick = onGotoArtistClick,
                                        offset = DpOffset(0.dp, (-20).dp),
                                    )
                                }
                            }
                        }
                    )
                    if (playerCurrentTrack == track) {
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
                            val artist = track.artist?.takeIf { it.isNotBlank() && showArtist }
                            val titleLines = if (artist != null) 1 else 2

                            Text(
                                text = track.title,
                                maxLines = titleLines,
                                minLines = titleLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (artist != null) {
                                Text(text = artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
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
    }
}
