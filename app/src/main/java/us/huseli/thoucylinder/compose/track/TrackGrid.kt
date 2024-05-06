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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ObnoxiousProgressIndicator
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.TrackUiState
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ImageViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackGrid(
    uiStates: LazyPagingItems<TrackUiState>,
    selectedTrackIds: ImmutableList<String>,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = hiltViewModel(),
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    showAlbum: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    trackCallbacks: (Int, TrackUiState) -> TrackCallbacks,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    progressIndicatorText: String? = null,
    ensureTrackMetadata: (TrackUiState) -> Unit,
    onEmpty: @Composable (() -> Unit)? = null,
) {
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
            items(count = uiStates.itemCount) { index ->
                uiStates[index]?.also { state ->
                    val isSelected = selectedTrackIds.contains(state.trackId)
                    val callbacks = trackCallbacks(index, state)
                    val downloadState = state.downloadState.collectAsStateWithLifecycle()

                    callbacks.onEach(state.trackId)

                    OutlinedCard(
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.combinedClickable(
                            onClick = { callbacks.onTrackClick(state.trackId) },
                            onLongClick = { callbacks.onLongClick(state.trackId) },
                        ),
                        border = CardDefaults.outlinedCardBorder()
                            .let { if (isSelected) it.copy(width = it.width + 2.dp) else it },
                    ) {
                        Box(modifier = Modifier.aspectRatio(1f)) {
                            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

                            LaunchedEffect(state) {
                                imageBitmap = imageViewModel.getThumbnailImageBitmap(state.trackThumbnailUri)
                                    ?: imageViewModel.getThumbnailImageBitmap(state.albumThumbnailUri)
                                ensureTrackMetadata(state)
                            }

                            Thumbnail(
                                imageBitmap = { imageBitmap },
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

                        downloadState.value?.also { state ->
                            if (state.isActive) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (downloadState.value?.isActive == true) 3.dp else 5.dp,
                                    bottom = 5.dp,
                                    start = 5.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val albumString = if (showAlbum) state.albumTitle else null
                                val artistString = if (showArtist) state.trackArtists.joined() else null
                                val titleLines = 1 + listOfNotNull(albumString, artistString).size

                                Text(
                                    text = state.title.umlautify(),
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
                                isDownloadable = state.isDownloadable,
                                isInLibrary = state.isInLibrary,
                                callbacks = callbacks,
                                artists = state.trackArtists,
                                youtubeWebUrl = state.youtubeWebUrl,
                                spotifyWebUrl = state.spotifyWebUrl,
                                trackId = state.trackId,
                                isPlayable = state.isPlayable,
                            )
                        }

                        downloadState.value?.also { state ->
                            val statusText = stringResource(state.status.stringId)

                            Column(modifier = Modifier.padding(bottom = 5.dp)) {
                                Text(text = "$statusText …")
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiStates.itemCount == 0 && onEmpty != null) onEmpty()
    }
}
