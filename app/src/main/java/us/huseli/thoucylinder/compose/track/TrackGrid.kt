package us.huseli.thoucylinder.compose.track

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.MusicNote
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.TrackDownloadTask
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.AbstractTrackCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.getDownloadProgress
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.AbstractBaseViewModel
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : AbstractTrackCombo> TrackGrid(
    trackCombos: LazyPagingItems<T>,
    selectedTrackIds: List<UUID>,
    trackDownloadTasks: List<TrackDownloadTask>,
    viewModel: AbstractBaseViewModel,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    trackCallbacks: (Int, T) -> TrackCallbacks<T>,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    progressIndicatorText: String? = null,
    onEmpty: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    SelectedTracksButtons(trackCount = selectedTrackIds.size, callbacks = trackSelectionCallbacks)

    Box {
        progressIndicatorText?.also {
            ObnoxiousProgressIndicator(text = it, modifier = Modifier.zIndex(1f))
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = contentPadding,
            modifier = modifier.padding(horizontal = 10.dp),
        ) {
            items(count = trackCombos.itemCount) { index ->
                trackCombos[index]?.also { combo ->
                    val track = combo.track
                    val isSelected = selectedTrackIds.contains(combo.track.trackId)
                    val (downloadProgress, downloadIsActive) =
                        getDownloadProgress(trackDownloadTasks.find { it.trackCombo.track.trackId == track.trackId })
                    val callbacks = trackCallbacks(index, combo)

                    callbacks.onEach?.invoke()

                    OutlinedCard(
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.combinedClickable(
                            onClick = { callbacks.onTrackClick?.invoke() },
                            onLongClick = { callbacks.onLongClick?.invoke() },
                        ),
                        border = CardDefaults.outlinedCardBorder()
                            .let { if (isSelected) it.copy(width = it.width + 2.dp) else it },
                    ) {
                        Box(modifier = Modifier.aspectRatio(1f)) {
                            val imageBitmap = remember(track) { mutableStateOf<ImageBitmap?>(null) }

                            LaunchedEffect(track) {
                                imageBitmap.value = combo.getFullImageBitmap(context)
                                viewModel.ensureTrackMetadata(track)
                            }

                            Thumbnail(
                                image = imageBitmap.value,
                                borderWidth = null,
                                placeholderIcon = Icons.Sharp.MusicNote,
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Sharp.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    tint = LocalBasicColors.current.Green.copy(alpha = 0.7f),
                                )
                            }
                        }

                        if (downloadIsActive) {
                            LinearProgressIndicator(
                                progress = { downloadProgress?.toFloat() ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (downloadIsActive) 3.dp else 5.dp,
                                    bottom = 5.dp,
                                    start = 5.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val albumString = if (showAlbum) combo.album?.title else null
                                val artistString = if (showArtist) combo.artists.joined() else null
                                val titleLines = 1 + listOfNotNull(albumString, artistString).size

                                Text(
                                    text = track.title.umlautify(),
                                    maxLines = titleLines,
                                    overflow = TextOverflow.Ellipsis,
                                    style = ThouCylinderTheme.typographyExtended.listSmallHeader,
                                )
                                if (artistString != null) Text(
                                    text = artistString.umlautify(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                )
                                if (albumString != null) Text(
                                    text = albumString.umlautify(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                )
                            }

                            TrackContextButtonWithMenu(
                                isDownloadable = track.isDownloadable,
                                isInLibrary = track.isInLibrary,
                                callbacks = callbacks,
                                trackArtists = combo.artists,
                            )
                        }

                        trackDownloadTasks.find { it.trackCombo.track == track }?.also { download ->
                            val status by download.downloadStatus.collectAsStateWithLifecycle()
                            val progress by download.downloadProgress.collectAsStateWithLifecycle()
                            val statusText = stringResource(status.stringId)

                            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                                Text(text = "$statusText …")
                                LinearProgressIndicator(
                                    progress = { progress.toFloat() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (trackCombos.itemCount == 0 && onEmpty != null) onEmpty()
    }
}
