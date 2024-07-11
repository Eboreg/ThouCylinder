package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.scrollbar.ScrollbarGridState
import us.huseli.thoucylinder.compose.scrollbar.rememberScrollbarGridState
import us.huseli.thoucylinder.compose.utils.DownloadStateProgressIndicator
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.track.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify

@Composable
fun TrackGrid(
    states: () -> ImmutableList<TrackUiState>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    selectedTrackCount: () -> Int,
    scrollbarState: ScrollbarGridState = rememberScrollbarGridState(),
    onClick: (TrackUiState) -> Unit,
    onLongClick: (TrackUiState) -> Unit,
    getDownloadStateFlow: (String) -> StateFlow<TrackDownloadTask.UiState?>,
    onEmpty: @Composable () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SelectedTracksButtons(trackCount = selectedTrackCount, callbacks = trackSelectionCallbacks)

    ItemGrid(
        things = states,
        key = { it.id },
        onClick = onClick,
        modifier = modifier,
        scrollbarState = scrollbarState,
        isLoading = isLoading,
        onLongClick = onLongClick,
        isSelected = { it.isSelected },
        onEmpty = onEmpty,
        trailingContent = trailingContent,
    ) { state ->
        val downloadStateFlow = remember(state.id) { getDownloadStateFlow(state.id) }
        val downloadState = downloadStateFlow.collectAsStateWithLifecycle()

        Box(modifier = Modifier.aspectRatio(1f)) {
            Thumbnail(
                model = state,
                borderWidth = null,
                placeholderIcon = Icons.Sharp.MusicNote,
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

        downloadState.value?.also { DownloadStateProgressIndicator(it) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val albumString = if (showAlbum) state.albumTitle else null
                val artistString = if (showArtist) state.artistString else null
                val titleLines = (if (showAlbum && showArtist) 3 else 2) -
                    listOfNotNull(albumString, artistString).size

                Text(
                    text = state.title.umlautify(),
                    maxLines = titleLines,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.primarySmallBold,
                )
                if (artistString != null) Text(
                    text = artistString.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.secondarySmall,
                )
                if (albumString != null) Text(
                    text = albumString.umlautify(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FistopyTheme.bodyStyles.secondarySmall,
                )
            }

            TrackBottomSheetWithButton(state = state)
        }

        downloadState.value?.also {
            val statusText = stringResource(it.status.stringId)

            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                Text(text = "$statusText …")
                DownloadStateProgressIndicator(downloadState = it)
            }
        }
    }
}
