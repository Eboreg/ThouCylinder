package us.huseli.thoucylinder.compose.track

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.DragHandle
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalFoundationApi::class)
@Composable
inline fun TrackListRow(
    state: TrackUiState,
    noinline thumbnail: () -> ImageBitmap?,
    showArtist: Boolean,
    showAlbum: Boolean,
    callbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    containerColor: Color? = null,
    reorderableState: ReorderableLazyListState? = null,
    downloadState: State<TrackDownloadTask.UiState?>? = null,
    crossinline extraContextMenuItems: @Composable () -> Unit = {},
) {
    callbacks.onEach(state.trackId)

    Card(
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
                ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.fillMaxWidth().height(50.dp).combinedClickable(
            onClick = { callbacks.onTrackClick(state.trackId) },
            onLongClick = { callbacks.onLongClick(state.trackId) },
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                imageBitmap = thumbnail,
                shape = MaterialTheme.shapes.extraSmall,
                placeholderIcon = Icons.Sharp.MusicNote,
                borderWidth = if (isSelected) null else 1.dp,
            )

            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val albumString = if (showAlbum) state.albumTitle else null
                        val artistString = if (showArtist) state.trackArtists.joined() else null
                        val secondRow = listOfNotNull(artistString, albumString, state.year?.toString())
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(" • ")

                        Text(
                            text = state.title.umlautify(),
                            maxLines = if (secondRow == null) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                        )
                        secondRow?.also {
                            Text(
                                text = it.umlautify(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listNormalTitleSecondary,
                            )
                        }
                    }

                    state.duration?.also { duration ->
                        Text(
                            text = duration.sensibleFormat(),
                            modifier = Modifier.padding(start = 5.dp),
                            style = ThouCylinderTheme.typographyExtended.listNormalTitle,
                        )
                    }

                    TrackContextButtonWithMenu(
                        trackId = state.trackId,
                        artists = state.trackArtists,
                        isDownloadable = state.isDownloadable,
                        isInLibrary = state.isInLibrary,
                        callbacks = callbacks,
                        extraItems = { extraContextMenuItems() },
                        youtubeWebUrl = state.youtubeWebUrl,
                        spotifyWebUrl = state.spotifyWebUrl,
                        isPlayable = state.isPlayable,
                    )

                    if (reorderableState != null) {
                        Icon(
                            Icons.Sharp.DragHandle,
                            null,
                            modifier = Modifier.detectReorder(reorderableState).height(18.dp).padding(end = 10.dp),
                        )
                    }
                }

                Row(modifier = Modifier.height(2.dp).fillMaxWidth()) {
                    downloadState?.value?.also { state ->
                        if (state.isActive) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
