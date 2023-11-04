package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.pojos.TrackPojo
import us.huseli.thoucylinder.viewmodels.AbstractBaseViewModel

@Composable
fun <T : TrackPojo> TrackList(
    trackPojos: LazyPagingItems<out T>,
    viewModel: AbstractBaseViewModel,
    selectedTracks: List<T>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    trackCallbacks: (T) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    extraTrackSelectionButtons: (@Composable () -> Unit)? = null,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    Column {
        SelectedTracksButtons(
            trackCount = selectedTracks.size,
            callbacks = trackSelectionCallbacks,
            extraButtons = extraTrackSelectionButtons,
        )

        ListWithNumericBar(
            listState = listState,
            listSize = trackPojos.itemCount,
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = contentPadding,
            ) {
                items(count = trackPojos.itemCount) { index ->
                    trackPojos[index]?.let { pojo ->
                        val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
                        var metadata by rememberSaveable { mutableStateOf(pojo.track.metadata) }

                        LaunchedEffect(pojo.track.trackId) {
                            thumbnail.value = viewModel.getTrackThumbnail(pojo, context)
                            if (metadata == null) metadata = viewModel.getTrackMetadata(pojo.track)
                        }

                        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                            TrackListRow(
                                title = pojo.track.title,
                                isDownloadable = pojo.track.isDownloadable,
                                modifier = modifier,
                                downloadProgress = downloadProgressMap[pojo.track.trackId],
                                thumbnail = thumbnail.value,
                                duration = metadata?.duration,
                                artist = if (showArtist) pojo.artist else null,
                                callbacks = trackCallbacks(pojo),
                                isSelected = selectedTracks.contains(pojo),
                            )
                        }
                    }
                }
            }
        }

        if (trackPojos.itemCount == 0) onEmpty?.invoke()
    }
}
