package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.CoroutineScope
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.dataclasses.DownloadProgress
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackList(
    tracks: LazyPagingItems<Track>,
    viewModel: BaseViewModel,
    listState: LazyListState = rememberLazyListState(),
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
    showArtist: Boolean = true,
) {
    TrackList(
        viewModel = viewModel,
        trackCount = tracks.itemCount,
        trackIterator = { action ->
            items(count = tracks.itemCount, key = tracks.itemKey { it.id }) { index ->
                tracks[index]?.let { track -> action(this, track) }
            }
        },
        onDownloadClick = onDownloadClick,
        onPlayOrPauseClick = onPlayOrPauseClick,
        listState = listState,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        showArtist = showArtist,
        onLaunch = onLaunch,
    )
}


@Composable
fun TrackList(
    tracks: List<Track>,
    viewModel: BaseViewModel,
    listState: LazyListState = rememberLazyListState(),
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
    showArtist: Boolean = true,
) {
    TrackList(
        viewModel = viewModel,
        trackCount = tracks.size,
        trackIterator = { action ->
            items(tracks) { track -> action(this, track) }
        },
        onDownloadClick = onDownloadClick,
        onPlayOrPauseClick = onPlayOrPauseClick,
        listState = listState,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        showArtist = showArtist,
        onLaunch = onLaunch,
    )
}


@Composable
fun TrackList(
    viewModel: BaseViewModel,
    trackCount: Int,
    trackIterator: LazyListScope.(@Composable LazyItemScope.(Track) -> Unit) -> Unit,
    listState: LazyListState,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onGotoArtistClick: ((String) -> Unit)?,
    onGotoAlbumClick: ((UUID) -> Unit)?,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)?,
    showArtist: Boolean,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val playerPlayingTrack by viewModel.playerPlayingTrack.collectAsStateWithLifecycle(null)

    ListWithNumericBar(listState = listState, listSize = trackCount) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            trackIterator { track ->
                if (onLaunch != null) LaunchedEffect(Unit) {
                    onLaunch(track)
                }

                ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                    TrackListRow(
                        track = track,
                        viewModel = viewModel,
                        showArtist = showArtist,
                        playerPlayingTrack = playerPlayingTrack,
                        downloadProgressMap = downloadProgressMap,
                        onDownloadClick = { onDownloadClick(track) },
                        onPlayOrPauseClick = { onPlayOrPauseClick(track) },
                        onGotoAlbumClick = onGotoAlbumClick,
                        onGotoArtistClick = onGotoArtistClick,
                    )
                }
            }
        }
    }
}


@Composable
fun TrackListRow(
    track: Track,
    viewModel: BaseViewModel,
    showArtist: Boolean,
    playerPlayingTrack: Track?,
    downloadProgressMap: Map<UUID, DownloadProgress>,
    onDownloadClick: () -> Unit,
    onPlayOrPauseClick: () -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
) {
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        track.image?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.fillMaxWidth().height(80.dp),
    ) {
        Row {
            AlbumArt(
                image = imageBitmap.value,
                modifier = Modifier.fillMaxHeight().padding(end = 10.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight().padding(vertical = 5.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = track.toString(showAlbumPosition = false, showArtist = showArtist),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                track.metadata?.let {
                    Text(text = it.duration.sensibleFormat(), modifier = Modifier.padding(start = 5.dp))
                }

                TrackContextMenuWithButton(
                    track = track,
                    onDownloadClick = onDownloadClick,
                    modifier = Modifier.padding(start = 10.dp).width(30.dp),
                    onGotoAlbumClick = onGotoAlbumClick,
                    onGotoArtistClick = onGotoArtistClick,
                )

                IconButton(
                    onClick = onPlayOrPauseClick,
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
