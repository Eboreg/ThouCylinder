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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Track
import us.huseli.thoucylinder.viewmodels.ImageViewModel

@Composable
fun TrackList(
    itemCount: Int,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 10.dp),
    trackSelectionCallbacks: TrackSelectionCallbacks? = null,
    selectedTrackIds: List<String> = emptyList(),
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
    selectedTrackIds: ImmutableList<String>,
    downloadStates: ImmutableList<TrackDownloadTask.ViewState>,
    trackCallbacks: (Int, Track.ViewState) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    progressIndicatorText: String? = null,
    ensureTrackMetadata: (Track) -> Unit,
    extraTrackSelectionButtons: (@Composable () -> Unit)? = null,
    onEmpty: (@Composable () -> Unit)? = null,
) {
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
                    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(combo.track.image, combo.album?.albumArt) {
                        thumbnail = imageViewModel.getTrackThumbnail(combo.track.image?.thumbnailUri)
                            ?: imageViewModel.getAlbumThumbnail(combo.album?.albumArt?.thumbnailUri)
                        ensureTrackMetadata(combo.track)
                        // thumbnail = viewModel.getTrackComboThumbnail(combo)
                        // viewModel.ensureTrackMetadata(combo.track)
                    }

                    TrackListRow(
                        combo = combo,
                        showArtist = showArtist,
                        isSelected = selectedTrackIds.contains(combo.track.trackId),
                        callbacks = remember { trackCallbacks(index, combo.getViewState()) },
                        thumbnail = { thumbnail },
                        showAlbum = showAlbum,
                        downloadState = downloadStates.find { it.trackId == combo.track.trackId },
                    )
                }
            }
        }
    }
}
