package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.viewmodels.AbstractTrackListViewModel
import java.util.UUID

@Composable
fun TrackList(
    itemCount: Int,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 10.dp),
    trackSelectionCallbacks: TrackSelectionCallbacks? = null,
    selectedTrackIds: List<UUID> = emptyList(),
    listState: LazyListState = rememberLazyListState(),
    extraTrackSelectionButtons: (@Composable () -> Unit)? = null,
    onEmpty: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column {
        trackSelectionCallbacks?.also { callbacks ->
            SelectedTracksButtons(
                trackCount = selectedTrackIds.size,
                callbacks = callbacks,
                extraButtons = extraTrackSelectionButtons,
            )
        }

        if (itemCount == 0 && onEmpty != null) onEmpty()

        ListWithNumericBar(
            listState = listState,
            listSize = itemCount,
            modifier = Modifier.padding(padding),
            itemHeight = 55.dp,
            minItems = 50,
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = modifier,
                content = content,
            )
        }
    }
}


@Composable
fun <T : AbstractTrackCombo> TrackList(
    trackCombos: LazyPagingItems<out T>,
    viewModel: AbstractTrackListViewModel,
    selectedTrackIds: List<UUID>,
    trackDownloadTasks: List<TrackDownloadTask>,
    trackCallbacks: (Int, T) -> TrackCallbacks<T>,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    progressIndicatorText: String? = null,
    extraTrackSelectionButtons: (@Composable () -> Unit)? = null,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    Box {
        progressIndicatorText?.also {
            ObnoxiousProgressIndicator(text = it, modifier = Modifier.zIndex(1f))
        }
        TrackList(
            itemCount = trackCombos.itemCount,
            selectedTrackIds = selectedTrackIds,
            trackSelectionCallbacks = trackSelectionCallbacks,
            listState = listState,
            modifier = modifier,
            extraTrackSelectionButtons = extraTrackSelectionButtons,
            onEmpty = onEmpty,
        ) {
            items(count = trackCombos.itemCount) { index ->
                trackCombos[index]?.also { combo ->
                    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(combo.track.image, combo.album?.albumArt) {
                        thumbnail.value = viewModel.getTrackThumbnail(combo, context)
                        viewModel.ensureTrackMetadata(combo.track)
                    }

                    TrackListRow(
                        combo = combo,
                        showArtist = showArtist,
                        isSelected = selectedTrackIds.contains(combo.track.trackId),
                        callbacks = remember { trackCallbacks(index, combo) },
                        downloadTask = trackDownloadTasks.find { it.trackCombo.track.trackId == combo.track.trackId },
                        thumbnail = thumbnail.value,
                        showAlbum = showAlbum,
                    )
                }
            }
        }
    }
}
