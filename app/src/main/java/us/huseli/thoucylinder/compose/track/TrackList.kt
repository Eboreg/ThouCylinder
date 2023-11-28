package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.viewmodels.AbstractTrackListViewModel

@Composable
fun <T : AbstractTrackPojo> TrackList(
    trackPojos: LazyPagingItems<out T>,
    viewModel: AbstractTrackListViewModel,
    selectedTrackPojos: List<T>,
    trackDownloadTasks: List<TrackDownloadTask>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    trackCallbacks: (T) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    extraTrackSelectionButtons: (@Composable () -> Unit)? = null,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    Column {
        SelectedTracksButtons(
            trackCount = selectedTrackPojos.size,
            callbacks = trackSelectionCallbacks,
            extraButtons = extraTrackSelectionButtons,
        )

        ListWithNumericBar(
            listState = listState,
            listSize = trackPojos.itemCount,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = contentPadding,
            ) {
                items(count = trackPojos.itemCount) { index ->
                    trackPojos[index]?.let { pojo ->
                        val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

                        LaunchedEffect(pojo.track.trackId) {
                            thumbnail.value = viewModel.getTrackThumbnail(pojo.track, pojo.album, context)
                            viewModel.ensureTrackMetadata(pojo.track, commit = true)
                        }

                        TrackListRow(
                            title = pojo.track.title,
                            isDownloadable = pojo.track.isDownloadable,
                            modifier = modifier,
                            downloadTask = trackDownloadTasks.find { it.track.trackId == pojo.track.trackId },
                            thumbnail = thumbnail.value,
                            duration = pojo.track.metadata?.duration,
                            artist = if (showArtist) pojo.artist else null,
                            callbacks = trackCallbacks(pojo),
                            isSelected = selectedTrackPojos.contains(pojo),
                        )
                    }
                }
            }
        }

        if (trackPojos.itemCount == 0 && onEmpty != null) onEmpty()
    }
}
