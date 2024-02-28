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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.retaintheme.extensions.sensibleFormat
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.abstr.joined
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.getDownloadProgress
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.umlautify
import java.util.UUID

@Composable
fun <T : AbstractAlbumCombo> AlbumList(
    combos: List<T>,
    albumCallbacks: (T) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    selectedAlbumIds: List<UUID>,
    albumDownloadTasks: List<AlbumDownloadTask>,
    showArtist: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    progressIndicatorStringRes: Int? = null,
    onEmpty: @Composable (() -> Unit)? = null,
    getThumbnail: suspend (T) -> ImageBitmap?,
) {
    val isSelected = { combo: T -> selectedAlbumIds.contains(combo.album.albumId) }

    Column {
        SelectedAlbumsButtons(albumCount = selectedAlbumIds.size, callbacks = albumSelectionCallbacks)

        ItemList(
            things = combos,
            isSelected = isSelected,
            onClick = { _, combo -> albumCallbacks(combo).onAlbumClick?.invoke() },
            onLongClick = { _, combo -> albumCallbacks(combo).onAlbumLongClick?.invoke() },
            onEmpty = onEmpty,
            listState = listState,
            cardHeight = 70.dp,
            progressIndicatorText = progressIndicatorStringRes?.let { stringResource(it) },
        ) { _, combo ->
            val (downloadProgress, downloadIsActive) =
                getDownloadProgress(albumDownloadTasks.find { it.album.albumId == combo.album.albumId })
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
            val thirdRow = listOfNotNull(
                pluralStringResource(R.plurals.x_tracks, combo.trackCount, combo.trackCount),
                combo.yearString,
                combo.duration?.sensibleFormat(),
            ).joinToString(" • ").nullIfBlank()
            val callbacks = albumCallbacks(combo)
            val artistString = combo.artists.joined()

            LaunchedEffect(combo.album.albumArt) {
                imageBitmap.value = getThumbnail(combo)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Thumbnail(
                    image = imageBitmap.value,
                    shape = MaterialTheme.shapes.extraSmall,
                    placeholderIcon = Icons.Sharp.Album,
                    borderWidth = if (isSelected(combo)) null else 1.dp,
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
                                text = combo.album.title.umlautify(),
                                maxLines = if (artistString != null && showArtist) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                            )
                            if (showArtist && artistString != null) Text(
                                text = artistString.umlautify(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                            )
                            if (thirdRow != null) {
                                Text(
                                    text = thirdRow,
                                    style = ThouCylinderTheme.typographyExtended.listNormalSubtitleSecondary,
                                    maxLines = 1,
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxHeight(),
                            content = { AlbumSmallIcons(combo = combo) },
                        )

                        AlbumContextMenuWithButton(
                            isLocal = combo.album.isLocal,
                            isInLibrary = combo.album.isInLibrary,
                            isPartiallyDownloaded = combo.isPartiallyDownloaded,
                            callbacks = callbacks,
                            albumArtists = combo.artists,
                        )
                    }

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
