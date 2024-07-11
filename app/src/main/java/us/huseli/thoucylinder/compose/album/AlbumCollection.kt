package us.huseli.thoucylinder.compose.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.glance.text.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarListState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.album.IAlbumUiState
import us.huseli.thoucylinder.stringResource

@Composable
fun AlbumCollection(
    displayType: DisplayType,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    selectedAlbumCount: () -> Int,
    selectionCallbacks: AlbumSelectionCallbacks,
    states: () -> ImmutableList<IAlbumUiState>,
    downloadStateFlow: (String) -> StateFlow<AlbumDownloadTask.UiState?>,
    modifier: Modifier = Modifier,
    showArtist: Boolean = true,
    isLoading: Boolean = false,
    scrollbarListState: ScrollbarListState = rememberScrollbarListState(),
    scrollbarGridState: ScrollbarGridState = rememberScrollbarGridState(),
    onEmpty: @Composable () -> Unit = { Text(stringResource(R.string.no_albums_found)) },
) {
    when (displayType) {
        DisplayType.LIST -> AlbumList(
            states = states,
            downloadStateFlow = downloadStateFlow,
            onClick = onClick,
            onLongClick = onLongClick,
            selectedAlbumCount = selectedAlbumCount,
            selectionCallbacks = selectionCallbacks,
            modifier = modifier,
            showArtist = showArtist,
            isLoading = isLoading,
            scrollbarState = scrollbarListState,
            onEmpty = onEmpty,
        )
        DisplayType.GRID -> AlbumGrid(
            states = states,
            downloadStateFlow = downloadStateFlow,
            onClick = onClick,
            onLongClick = onLongClick,
            selectedAlbumCount = selectedAlbumCount,
            selectionCallbacks = selectionCallbacks,
            modifier = modifier,
            isLoading = isLoading,
            scrollbarState = scrollbarGridState,
            showArtist = showArtist,
            onEmpty = onEmpty,
        )
    }
}
