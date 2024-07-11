package us.huseli.thoucylinder.compose.modalcover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.isInLandscapeMode
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.ModalCoverBooleans
import us.huseli.thoucylinder.dataclasses.callbacks.PlaybackCallbacks

@Composable
fun AlbumArtAndTitlesColumn(
    state: ModalCoverState,
    albumArtModel: Any?,
    title: String,
    artist: String?,
    offsetX: Int,
    playbackCallbacks: PlaybackCallbacks,
    booleans: ModalCoverBooleans,
    modifier: Modifier = Modifier,
) {
    val isLandscape = isInLandscapeMode()

    Column(
        modifier = modifier.offset { IntOffset(x = offsetX, y = 0) }.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(state.albumArtContainerWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Thumbnail(
                    model = albumArtModel,
                    placeholderIcon = Icons.Sharp.MusicNote,
                    modifier = Modifier.heightIn(max = state.albumArtMaxSize).height(state.albumArtSize),
                    customModelSize = state.albumArtModelSize,
                )
            }

            // To the right of album art: Titles and buttons for collapsed, just titles for
            // expanded landscape, nothing for expanded portrait:
            if (state.isCollapsed || state.isAnimating || isLandscape) {
                val alpha by remember {
                    derivedStateOf { ((state.collapseProgress * 2) - 1f).coerceAtLeast(0f) }
                }

                TitlesColumn(
                    state = state,
                    modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
                    title = title,
                    artist = artist,
                    horizontalAlignment = Alignment.Start,
                    padding = PaddingValues(top = 0.dp),
                    alpha = if (!isLandscape) alpha else 1f,
                )
                if (!state.isExpanded) CollapsedPlayerButtons(
                    booleans = booleans,
                    playbackCallbacks = playbackCallbacks,
                    alpha = alpha,
                )
            }
        }

        if (!isLandscape && !state.isCollapsed) TitlesColumn(
            title = title,
            artist = artist,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            state = state,
        )
    }
}
