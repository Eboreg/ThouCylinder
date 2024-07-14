package us.huseli.thoucylinder.compose.imports

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarListState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarListState
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.album.ImportableAlbumUiState
import us.huseli.thoucylinder.externalcontent.ImportBackend
import us.huseli.thoucylinder.stringResource

@Composable
fun ImportableAlbumList(
    uiStates: ImmutableList<ImportableAlbumUiState>,
    isLoadingCurrentPage: Boolean,
    isEmpty: Boolean,
    backend: ImportBackend,
    onGotoAlbumClick: (String) -> Unit,
    toggleSelected: (String) -> Unit,
    onLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(),
) {
    LaunchedEffect(uiStates.firstOrNull()) {
        if (uiStates.isNotEmpty()) scrollbarState.scrollToItem(0)
    }

    ItemList(
        things = { uiStates },
        key = { it.id },
        modifier = modifier,
        scrollbarState = scrollbarState,
        isLoading = isLoadingCurrentPage,
        loadingText = stringResource(R.string.loading),
        onEmpty = { if (isEmpty) Text(stringResource(R.string.no_albums_found)) },
        contentType = "IExternalAlbumWithTracks",
    ) { state ->
        ImportableAlbumCard(
            state = state,
            onClick = {
                if (state.isSaved) onGotoAlbumClick(state.albumId)
                else toggleSelected(state.id)
            },
            onLongClick = { onLongClick(state.id) },
            backend = backend,
        )
    }
}
