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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
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
fun TrackList(
    uiStates: LazyPagingItems<TrackUiState>,
    trackCallbacks: (Int, TrackUiState) -> TrackCallbacks,
    selectedTrackIds: ImmutableList<String>,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    progressIndicatorText: String? = null,
    ensureTrackMetadata: (TrackUiState) -> Unit,
    extraTrackSelectionButtons: @Composable (() -> Unit)? = null,
    onEmpty: @Composable (() -> Unit)? = null,
) {
    Box {
        progressIndicatorText?.also {
            ObnoxiousProgressIndicator(text = it, modifier = Modifier.zIndex(1f))
        }
        TrackList(
            itemCount = uiStates.itemCount,
            selectedTrackIds = selectedTrackIds,
            trackSelectionCallbacks = trackSelectionCallbacks,
            listState = listState,
            modifier = modifier,
            extraTrackSelectionButtons = extraTrackSelectionButtons,
            onEmpty = onEmpty,
        ) {
            items(count = uiStates.itemCount) { index ->
                uiStates[index]?.also { state ->
                    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
                    val downloadState = state.downloadState.collectAsStateWithLifecycle()

                    LaunchedEffect(state.trackThumbnailUri, state.albumThumbnailUri) {
                        thumbnail = imageViewModel.getThumbnailImageBitmap(state.trackThumbnailUri)
                            ?: imageViewModel.getThumbnailImageBitmap(state.albumThumbnailUri)
                        ensureTrackMetadata(state)
                    }

                    TrackListRow(
                        showArtist = showArtist,
                        isSelected = selectedTrackIds.contains(state.trackId),
                        thumbnail = { thumbnail },
                        showAlbum = showAlbum,
                        downloadState = downloadState,
                        callbacks = remember { trackCallbacks(index, state) },
                        state = state,
                    )
                }
            }
        }
    }
}
