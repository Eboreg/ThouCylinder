package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarListState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.ItemListReorderable
import us.huseli.thoucylinder.compose.utils.SelectionAction
import us.huseli.thoucylinder.dataclasses.track.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.track.TrackUiState

@Composable
fun TrackList(
    states: () -> ImmutableList<TrackUiState>,
    getDownloadStateFlow: (String) -> StateFlow<TrackDownloadTask.UiState?>,
    onClick: (TrackUiState) -> Unit,
    onLongClick: (TrackUiState) -> Unit,
    selectedTrackCount: () -> Int,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(),
    showAlbum: Boolean = false,
    showArtist: Boolean = true,
    onEmpty: @Composable () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SelectedTracksButtons(
        trackCount = selectedTrackCount,
        callbacks = trackSelectionCallbacks,
    )

    ItemList(
        things = states,
        key = { it.id },
        modifier = modifier,
        scrollbarState = scrollbarState,
        isLoading = isLoading,
        onEmpty = onEmpty,
        trailingContent = trailingContent,
        contentType = "TrackUiState",
    ) { state ->
        TrackListCard(
            state = state,
            downloadStateFlow = remember { getDownloadStateFlow(state.trackId) },
            showAlbum = showAlbum,
            showArtist = showArtist,
            onClick = { onClick(state) },
            onLongClick = { onLongClick(state) },
        )
    }
}

@Composable
fun TrackListReorderable(
    states: () -> ImmutableList<TrackUiState>,
    getDownloadStateFlow: (String) -> StateFlow<TrackDownloadTask.UiState?>,
    onClick: (TrackUiState) -> Unit,
    onLongClick: (TrackUiState) -> Unit,
    selectedTrackCount: () -> Int,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    reorderableState: ReorderableLazyListState,
    modifier: Modifier = Modifier,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(listState = reorderableState.listState),
    isLoading: Boolean = false,
    showAlbum: Boolean = false,
    showArtist: Boolean = true,
    extraBottomSheetItems: @Composable (TrackUiState) -> Unit = {},
    extraSelectionActions: ImmutableList<SelectionAction> = persistentListOf(),
    onEmpty: @Composable () -> Unit = {},
    containerColor: @Composable ((TrackUiState, Boolean) -> Color)? = null,
) {
    SelectedTracksButtons(
        trackCount = selectedTrackCount,
        callbacks = trackSelectionCallbacks,
        extraActions = extraSelectionActions,
    )

    ItemListReorderable(
        things = states,
        key = { it.id },
        reorderableState = reorderableState,
        scrollbarState = scrollbarState,
        modifier = modifier.fillMaxHeight(),
        isLoading = isLoading,
        onEmpty = onEmpty,
        contentType = "TrackUiState",
    ) { state, isDragging ->
        TrackListCard(
            state = state,
            downloadStateFlow = remember { getDownloadStateFlow(state.trackId) },
            showAlbum = showAlbum,
            showArtist = showArtist,
            onClick = { onClick(state) },
            onLongClick = { onLongClick(state) },
            extraIcons = {
                Icon(
                    Icons.Sharp.DragHandle,
                    null,
                    modifier = Modifier.detectReorder(reorderableState).height(18.dp).padding(end = 10.dp),
                )
            },
            extraBottomSheetItems = { extraBottomSheetItems(state) },
            containerColor = when {
                containerColor != null -> containerColor(state, isDragging)
                isDragging -> MaterialTheme.colorScheme.surface
                state.isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Unspecified
            },
        )
    }
}
