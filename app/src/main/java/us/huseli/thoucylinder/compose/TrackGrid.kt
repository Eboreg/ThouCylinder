package us.huseli.thoucylinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackPojo
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.viewmodels.AbstractBaseViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : AbstractTrackPojo> TrackGrid(
    trackPojos: LazyPagingItems<T>,
    selectedTracks: List<T>,
    viewModel: AbstractBaseViewModel,
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    trackCallbacks: (T) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()

    SelectedTracksButtons(trackCount = selectedTracks.size, callbacks = trackSelectionCallbacks)

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 160.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding,
        modifier = Modifier.padding(horizontal = 10.dp),
    ) {
        items(count = trackPojos.itemCount) { index ->
            trackPojos[index]?.also { pojo ->
                val track = pojo.track
                val isSelected = selectedTracks.contains(pojo)

                OutlinedCard(
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.combinedClickable(
                        onClick = { trackCallbacks(pojo).onTrackClick?.invoke() },
                        onLongClick = { trackCallbacks(pojo).onLongClick?.invoke() },
                    ),
                    border = CardDefaults.outlinedCardBorder()
                        .let { if (isSelected) it.copy(width = it.width + 2.dp) else it },
                ) {
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
                        var metadata by rememberSaveable { mutableStateOf(track.metadata) }

                        LaunchedEffect(Unit) {
                            imageBitmap.value = pojo.getFullImage(context)
                            if (metadata == null) metadata = viewModel.getTrackMetadata(track)
                        }

                        Thumbnail(image = imageBitmap.value, borderWidth = null)

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Sharp.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                tint = LocalBasicColors.current.Green.copy(alpha = 0.7f),
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.padding(5.dp).weight(1f)) {
                            val artist = track.artist?.takeIf { it.isNotBlank() && showArtist }
                            val titleLines = if (artist != null) 1 else 2

                            Text(
                                text = track.title,
                                maxLines = titleLines,
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

                        TrackContextMenuWithButton(
                            isDownloadable = track.isDownloadable,
                            callbacks = trackCallbacks(pojo),
                        )
                    }

                    downloadProgressMap[track.trackId]?.let { progress ->
                        val statusText = stringResource(progress.status.stringId)

                        Column(modifier = Modifier.padding(bottom = 5.dp)) {
                            Text(text = "$statusText …")
                            LinearProgressIndicator(
                                progress = progress.progress.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (trackPojos.itemCount == 0 && onEmpty != null) onEmpty()
}
