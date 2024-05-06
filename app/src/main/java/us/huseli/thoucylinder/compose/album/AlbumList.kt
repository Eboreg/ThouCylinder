package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.uistates.AlbumUiState
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ImageViewModel

@Composable
fun AlbumList(
    states: () -> ImmutableList<AlbumUiState>,
    callbacks: AlbumCallbacks,
    selectionCallbacks: AlbumSelectionCallbacks,
    selectedAlbumIds: ImmutableList<String>,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = hiltViewModel(),
    showArtist: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    progressIndicatorText: () -> String? = { null },
    onEmpty: @Composable (() -> Unit)? = null,
) {
    val isSelected =
        remember(selectedAlbumIds) { { state: AlbumUiState -> selectedAlbumIds.contains(state.albumId) } }

    Column {
        SelectedAlbumsButtons(albumCount = selectedAlbumIds.size, callbacks = selectionCallbacks)

        ItemList(
            modifier = modifier,
            things = states(),
            isSelected = isSelected,
            onClick = { _, state -> callbacks.onAlbumClick?.invoke(state.albumId) },
            onLongClick = { _, state -> callbacks.onAlbumLongClick?.invoke(state.albumId) },
            onEmpty = onEmpty,
            listState = listState,
            cardHeight = 70.dp,
            progressIndicatorText = progressIndicatorText,
        ) { _, state ->
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            val thirdRow = listOfNotNull(
                pluralStringResource(R.plurals.x_tracks, state.trackCount, state.trackCount),
                state.yearString,
                state.duration?.sensibleFormat(),
            ).joinToString(" • ").nullIfBlank()
            val downloadState by state.downloadState.collectAsStateWithLifecycle()
            val artistString = remember(state) { state.artists.joined() }

            LaunchedEffect(state.thumbnailUri) {
                imageBitmap = imageViewModel.getThumbnailImageBitmap(state.thumbnailUri)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Thumbnail(
                    imageBitmap = { imageBitmap },
                    shape = MaterialTheme.shapes.extraSmall,
                    placeholderIcon = Icons.Sharp.Album,
                    borderWidth = if (isSelected(state)) null else 1.dp,
                )

                Column {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Text(
                                text = state.title.umlautify(),
                                maxLines = if (artistString != null && showArtist) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                            )
                            if (showArtist && artistString != null) Text(
                                text = artistString.umlautify(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listSmallTitle,
                            )
                            if (thirdRow != null) {
                                Text(
                                    text = thirdRow,
                                    style = ThouCylinderTheme.typographyExtended.listSmallTitleSecondary,
                                    maxLines = 1,
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight(),
                            content = {
                                AlbumSmallIcons(
                                    isLocal = state.isLocal,
                                    isOnYoutube = state.isOnYoutube,
                                )
                            },
                        )

                        AlbumContextMenuWithButton(
                            albumId = state.albumId,
                            albumArtists = state.artists,
                            isLocal = state.isLocal,
                            isInLibrary = state.isInLibrary,
                            isPartiallyDownloaded = state.isPartiallyDownloaded,
                            callbacks = callbacks,
                            spotifyWebUrl = state.spotifyWebUrl,
                            youtubeWebUrl = state.youtubeWebUrl,
                        )
                    }

                    downloadState?.also {
                        if (it.isActive) LinearProgressIndicator(
                            progress = { it.progress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                        )
                    }
                }
            }
        }
    }
}
