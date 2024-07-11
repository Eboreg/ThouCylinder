package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarGridState
import us.huseli.thoucylinder.compose.utils.DownloadStateProgressIndicator
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.album.IAlbumUiState
import us.huseli.thoucylinder.umlautify

@Composable
fun AlbumGrid(
    states: () -> ImmutableList<IAlbumUiState>,
    downloadStateFlow: (String) -> StateFlow<AlbumDownloadTask.UiState?>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    selectedAlbumCount: () -> Int,
    selectionCallbacks: AlbumSelectionCallbacks,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    scrollbarState: ScrollbarGridState = rememberScrollbarGridState(),
    showArtist: Boolean = true,
    onEmpty: @Composable () -> Unit = {},
) {
    SelectedAlbumsButtons(albumCount = selectedAlbumCount, callbacks = selectionCallbacks)

    ItemGrid(
        things = states,
        modifier = modifier,
        onClick = { onClick(it.albumId) },
        onLongClick = { onLongClick(it.albumId) },
        isSelected = { it.isSelected },
        isLoading = isLoading,
        key = { it.albumId },
        scrollbarState = scrollbarState,
        onEmpty = onEmpty,
    ) { state ->
        AlbumGridCell(
            state = state,
            downloadStateFlow = { downloadStateFlow(state.albumId) },
            showArtist = showArtist,
        )
    }
}

@Composable
fun AlbumGridCell(
    state: IAlbumUiState,
    downloadStateFlow: () -> StateFlow<AlbumDownloadTask.UiState?>,
    showArtist: Boolean = true,
) {
    val downloadState by downloadStateFlow().collectAsStateWithLifecycle()

    Box(modifier = Modifier.aspectRatio(1f)) {
        Thumbnail(
            model = state,
            placeholderIcon = Icons.Sharp.Album,
            borderWidth = null,
            shape = RectangleShape,
        )

        if (state.isSelected) {
            Icon(
                imageVector = Icons.Sharp.CheckCircle,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(10.dp),
                tint = LocalBasicColors.current.Green.copy(alpha = 0.7f),
            )
        }
    }

    downloadState?.also { DownloadStateProgressIndicator(it) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(10.dp).height(56.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = state.title.umlautify(),
                maxLines = if (state.artistString != null && showArtist) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                style = FistopyTheme.bodyStyles.primaryBold,
            )
            if (showArtist) state.artistString?.also {
                Text(
                    text = it.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primarySmall,
                )
            }
        }

        AlbumBottomSheetWithButton(uiState = state)
    }
}
