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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorder
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.ProgressData
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import kotlin.time.Duration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListRow(
    title: String,
    artist: String?,
    duration: Duration?,
    thumbnail: ImageBitmap?,
    isSelected: Boolean,
    isDownloadable: Boolean,
    callbacks: TrackCallbacks,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    reorderableState: ReorderableLazyListState? = null,
    progressData: ProgressData? = null,
    extraContextMenuItems: (@Composable () -> Unit)? = null,
) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight().padding(vertical = 5.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = title,
                        maxLines = if (artist == null) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ThouCylinderTheme.typographyExtended.listSmallHeader,
                    )
                    if (artist != null) {
                        Text(
                            text = artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                        )
                    }
                }

                if (duration != null) {
                    Text(
                        text = duration.sensibleFormat(),
                        modifier = Modifier.padding(start = 5.dp),
                        style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                    )
                }

                TrackContextMenuWithButton(
                    isDownloadable = isDownloadable,
                    callbacks = callbacks,
                    extraItems = extraContextMenuItems,
                )

                if (reorderableState != null) {
                    Icon(
                        Icons.Sharp.DragHandle,
                        null,
                        modifier = Modifier.detectReorder(reorderableState).height(18.dp).padding(end = 10.dp),
                    )
                }
            }
        }

        if (progressData != null) {
            val statusText = stringResource(progressData.status.stringId)

            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                Text(text = "$statusText …")
                LinearProgressIndicator(
                    progress = progressData.progress.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
