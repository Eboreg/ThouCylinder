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
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.getDownloadProgress
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalFoundationApi::class)
@Composable
inline fun <T : AbstractTrackCombo> TrackListRow(
    combo: T,
    thumbnail: ImageBitmap?,
    showArtist: Boolean,
    isSelected: Boolean,
    callbacks: TrackCallbacks<*>,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    reorderableState: ReorderableLazyListState? = null,
    downloadTask: TrackDownloadTask? = null,
    crossinline extraContextMenuItems: @Composable () -> Unit = {},
) {
    val artist = if (showArtist) combo.artist else null
    val (downloadProgress, downloadIsActive) = getDownloadProgress(downloadTask)

    callbacks.onEach?.invoke()

    Card(
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
                ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.fillMaxWidth().height(50.dp).combinedClickable(
            onClick = { callbacks.onTrackClick?.invoke() },
            onLongClick = callbacks.onLongClick,
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Thumbnail(
                image = thumbnail,
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = combo.track.title.umlautify(),
                            maxLines = if (artist == null) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                        )
                        if (artist != null) {
                            Text(
                                text = artist.umlautify(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listNormalTitleSecondary,
                            )
                        }
                    }

                    combo.track.duration?.also { duration ->
                        Text(
                            text = duration.sensibleFormat(),
                            modifier = Modifier.padding(start = 5.dp),
                            style = ThouCylinderTheme.typographyExtended.listNormalTitle,
                        )
                    }

                    TrackContextButtonWithMenu(
                        isDownloadable = combo.track.isDownloadable,
                        callbacks = callbacks,
                        extraItems = extraContextMenuItems,
                        isInLibrary = combo.track.isInLibrary,
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
                    if (downloadIsActive) {
                        LinearProgressIndicator(
                            progress = { downloadProgress?.toFloat() ?: 0f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                        )
                    }
                }
            }
        }
    }
}
