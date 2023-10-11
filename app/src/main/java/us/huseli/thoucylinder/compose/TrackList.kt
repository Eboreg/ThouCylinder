package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackList(
    pojos: LazyPagingItems<TrackPojo>,
    viewModel: BaseViewModel,
    onDownloadClick: (Track) -> Unit,
    onPlayClick: (Track) -> Unit,
    onAddToPlaylistClick: (Selection) -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((UUID) -> Unit)? = null,
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val selectedTracks by viewModel.selectedTracks.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            trackCount = selectedTracks.size,
            onAddToPlaylistClick = { onAddToPlaylistClick(Selection(tracks = selectedTracks)) },
            onUnselectAllClick = { viewModel.unselectAllTracks() },
        )

        ListWithNumericBar(listState = listState, listSize = pojos.itemCount) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = contentPadding,
            ) {
                items(count = pojos.itemCount) { index ->
                    pojos[index]?.let { pojo ->
                        val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
                        var metadata by rememberSaveable { mutableStateOf(pojo.track.metadata) }

                        LaunchedEffect(pojo.track.trackId) {
                            pojo.track.image?.let { thumbnail.value = viewModel.getImageBitmap(it) }
                            if (metadata == null) metadata = viewModel.getTrackMetadata(pojo.track)
                        }

                        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                            TrackListRow(
                                track = pojo.track,
                                album = pojo.album,
                                metadata = metadata,
                                showArtist = showArtist,
                                onDownloadClick = { onDownloadClick(pojo.track) },
                                onPlayClick = { onPlayClick(pojo.track) },
                                modifier = cardModifier,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                                downloadProgress = downloadProgressMap[pojo.track.trackId],
                                onAddToPlaylistClick = { onAddToPlaylistClick(Selection(pojo.track)) },
                                onToggleSelected = { viewModel.toggleSelected(pojo.track) },
                                isSelected = selectedTracks.contains(pojo.track),
                                selectOnShortClick = selectedTracks.isNotEmpty(),
                                thumbnail = thumbnail.value,
                            )
                        }
                    }
                }
            }
        }
    }
}
