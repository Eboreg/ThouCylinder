package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackList(
    tracks: LazyPagingItems<Track>,
    viewModel: BaseViewModel,
    onDownloadClick: (Track) -> Unit,
    onPlayClick: (Track) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    showArtist: Boolean = true,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            selection = selection,
            onAddToPlaylistClick = { onAddToPlaylistClick(selection) },
            onUnselectAllClick = { viewModel.unselectAllTracks() },
        )

        ListWithNumericBar(listState = listState, listSize = tracks.itemCount) {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(count = tracks.itemCount, key = tracks.itemKey { it.trackId }) { index ->
                    tracks[index]?.let { track ->
                        val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
                        var metadata by rememberSaveable { mutableStateOf(track.metadata) }

                        LaunchedEffect(track.trackId) {
                            track.image?.let { thumbnail.value = viewModel.getImageBitmap(it) }
                            if (metadata == null) metadata = viewModel.getTrackMetadata(track)
                        }

                        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                            TrackListRow(
                                track = track,
                                metadata = metadata,
                                showArtist = showArtist,
                                onDownloadClick = { onDownloadClick(track) },
                                onPlayClick = { onPlayClick(track) },
                                modifier = cardModifier,
                                onGotoArtistClick = onGotoArtistClick,
                                onGotoAlbumClick = onGotoAlbumClick,
                                downloadProgress = downloadProgressMap[track.trackId],
                                onAddToPlaylistClick = { onAddToPlaylistClick(Selection(track)) },
                                onToggleSelected = { viewModel.toggleSelected(track) },
                                isSelected = selection.isSelected(track),
                                selectOnShortClick = selection.tracks.isNotEmpty(),
                                thumbnail = thumbnail.value,
                            )
                        }
                    }
                }
            }
        }
    }
}
