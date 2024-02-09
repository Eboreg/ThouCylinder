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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.viewmodels.AbstractTrackListViewModel

@Composable
fun <T : AbstractTrackCombo> TrackList(
    trackCombos: LazyPagingItems<out T>,
    viewModel: AbstractTrackListViewModel,
    selectedTrackCombos: List<T>,
    trackDownloadTasks: List<TrackDownloadTask>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    trackCallbacks: (Int, T) -> TrackCallbacks<T>,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    extraTrackSelectionButtons: (@Composable () -> Unit)? = null,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    Column {
        SelectedTracksButtons(
            trackCount = selectedTrackCombos.size,
            callbacks = trackSelectionCallbacks,
            extraButtons = extraTrackSelectionButtons,
        )

        if (trackCombos.itemCount == 0 && onEmpty != null) onEmpty()

        ListWithNumericBar(
            listState = listState,
            listSize = trackCombos.itemCount,
            modifier = Modifier.padding(horizontal = 10.dp),
            itemHeight = 55.dp,
            minItems = 50,
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = contentPadding,
            ) {
                items(count = trackCombos.itemCount) { index ->
                    trackCombos[index]?.let { combo ->
                        val thumbnail = remember(combo.track) { mutableStateOf<ImageBitmap?>(null) }

                        LaunchedEffect(combo.track) {
                            thumbnail.value = viewModel.getTrackThumbnail(
                                track = combo.track,
                                album = combo.album,
                                context = context,
                            )
                            viewModel.ensureTrackMetadata(combo.track)
                        }

                        TrackListRow(
                            title = combo.track.title,
                            isDownloadable = combo.track.isDownloadable,
                            modifier = modifier,
                            downloadTask = trackDownloadTasks.find { it.track.trackId == combo.track.trackId },
                            thumbnail = thumbnail.value,
                            duration = combo.track.metadata?.duration,
                            artist = if (showArtist) combo.artist else null,
                            callbacks = trackCallbacks(index, combo),
                            isSelected = selectedTrackCombos.contains(combo),
                            isInLibrary = combo.track.isInLibrary,
                        )
                    }
                }
            }
        }
    }
}
