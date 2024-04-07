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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import us.huseli.thoucylinder.viewmodels.ImageViewModel

@Composable
fun AlbumList(
    states: ImmutableList<Album.ViewState>,
    callbacks: (Album.ViewState) -> AlbumCallbacks,
    selectionCallbacks: AlbumSelectionCallbacks,
    selectedAlbumIds: ImmutableList<String>,
    downloadStates: ImmutableList<AlbumDownloadTask.ViewState>,
    modifier: Modifier = Modifier,
    imageViewModel: ImageViewModel = hiltViewModel(),
    showArtist: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    progressIndicatorStringRes: Int? = null,
    onEmpty: @Composable (() -> Unit)? = null,
) {
    val isSelected = { state: Album.ViewState -> selectedAlbumIds.contains(state.album.albumId) }

    Column {
        SelectedAlbumsButtons(albumCount = selectedAlbumIds.size, callbacks = selectionCallbacks)

        ItemList(
            modifier = modifier,
            things = states,
            isSelected = isSelected,
            onClick = { _, combo -> callbacks(combo).onAlbumClick?.invoke() },
            onLongClick = { _, combo -> callbacks(combo).onAlbumLongClick?.invoke() },
            onEmpty = onEmpty,
            listState = listState,
            cardHeight = 70.dp,
            progressIndicatorText = progressIndicatorStringRes?.let { stringResource(it) },
        ) { _, state ->
            var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            val thirdRow = listOfNotNull(
                pluralStringResource(R.plurals.x_tracks, state.trackCount, state.trackCount),
                state.yearString,
            ).joinToString(" • ").nullIfBlank()
            val albumCallbacks = remember(state) { callbacks(state) }
            val downloadState = downloadStates.find { it.albumId == state.album.albumId }
            val artistString = remember(state) { state.artists.joined() }

            LaunchedEffect(state.album.albumArt) {
                imageBitmap = imageViewModel.getAlbumThumbnail(state.album.albumArt?.thumbnailUri)
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
                                text = state.album.title.umlautify(),
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
                                    isLocal = state.album.isLocal,
                                    isOnYoutube = state.album.isOnYoutube,
                                )
                            },
                        )

                        AlbumContextMenuWithButton(
                            isLocal = state.album.isLocal,
                            isInLibrary = state.album.isInLibrary,
                            isPartiallyDownloaded = state.isPartiallyDownloaded,
                            callbacks = albumCallbacks,
                            albumArtists = state.artists.toImmutableList(),
                            youtubeWebUrl = state.album.youtubeWebUrl,
                            spotifyWebUrl = state.album.spotifyWebUrl,
                        )
                    }

                    if (downloadState?.isActive == true) {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                        )
                    }
                }
            }
        }
    }
}
