package us.huseli.thoucylinder.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.CoroutineScope
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackList(
    tracks: LazyPagingItems<Track>,
    viewModel: BaseViewModel,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    @SuppressLint("ModifierParameter") cardModifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
    showArtist: Boolean = true,
) {
    TrackList(
        viewModel = viewModel,
        trackCount = tracks.itemCount,
        trackIterator = { action ->
            items(count = tracks.itemCount, key = tracks.itemKey { it.trackId }) { index ->
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
        cardModifier = cardModifier,
        onAddToPlaylistClick = onAddToPlaylistClick,
    )
}


@Composable
fun TrackList(
    tracks: List<Track>,
    viewModel: BaseViewModel,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    @SuppressLint("ModifierParameter") cardModifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
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
        cardModifier = cardModifier,
        onAddToPlaylistClick = onAddToPlaylistClick,
    )
}


@Composable
fun TrackList(
    viewModel: BaseViewModel,
    trackCount: Int,
    trackIterator: LazyListScope.(@Composable LazyItemScope.(Track) -> Unit) -> Unit,
    listState: LazyListState,
    @SuppressLint("ModifierParameter") cardModifier: Modifier,
    onDownloadClick: (Track) -> Unit,
    onPlayOrPauseClick: (Track) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    onGotoArtistClick: ((String) -> Unit)?,
    onGotoAlbumClick: ((UUID) -> Unit)?,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)?,
    showArtist: Boolean,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val playerPlayingTrack by viewModel.playerPlayingTrack.collectAsStateWithLifecycle(null)
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            selection = selection,
            onAddToPlaylistClick = { onAddToPlaylistClick(selection) },
            onUnselectAllClick = { viewModel.unselectAllTracks() },
        )

        ListWithNumericBar(listState = listState, listSize = trackCount) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                trackIterator { track ->
                    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(track.trackId) {
                        track.image?.let { thumbnail.value = viewModel.getImageBitmap(it) }
                    }

                    if (onLaunch != null) LaunchedEffect(track.trackId) {
                        onLaunch(track)
                    }

                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                        TrackListRow(
                            track = track,
                            showArtist = showArtist,
                            onDownloadClick = { onDownloadClick(track) },
                            onPlayOrPauseClick = { onPlayOrPauseClick(track) },
                            modifier = cardModifier,
                            onGotoArtistClick = onGotoArtistClick,
                            onGotoAlbumClick = onGotoAlbumClick,
                            downloadProgress = downloadProgressMap[track.trackId],
                            isPlaying = playerPlayingTrack?.trackId == track.trackId,
                            onAddToPlaylistClick = { onAddToPlaylistClick(Selection(tracks = listOf(track))) },
                            onToggleSelected = { viewModel.toggleTrackSelected(track) },
                            isSelected = selection.isTrackSelected(track),
                            selectOnShortClick = selection.tracks.isNotEmpty(),
                            thumbnail = thumbnail.value,
                        )
                    }
                }
            }
        }
    }
}
