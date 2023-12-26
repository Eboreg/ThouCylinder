package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.retaintheme.ui.theme.LocalBasicColors
import us.huseli.thoucylinder.AlbumDownloadTask
import us.huseli.thoucylinder.ThouCylinderTheme
import us.huseli.thoucylinder.compose.utils.ItemGrid
import us.huseli.thoucylinder.compose.utils.Thumbnail
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumPojo
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumSelectionCallbacks
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.getDownloadProgress

@Composable
fun <T : AbstractAlbumPojo> AlbumGrid(
    pojos: List<T>,
    albumCallbacks: (T) -> AlbumCallbacks,
    albumSelectionCallbacks: AlbumSelectionCallbacks,
    selectedAlbums: List<Album>,
    showArtist: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    albumDownloadTasks: List<AlbumDownloadTask>,
    onEmpty: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSelected: (T) -> Boolean = { selectedAlbums.contains(it.album) }

    SelectedAlbumsButtons(albumCount = selectedAlbums.size, callbacks = albumSelectionCallbacks)

    ItemGrid(
        things = pojos,
        onClick = { _, pojo -> albumCallbacks(pojo).onAlbumClick?.invoke() },
        onLongClick = { _, pojo -> albumCallbacks(pojo).onAlbumLongClick?.invoke() },
        contentPadding = contentPadding,
        onEmpty = onEmpty,
        key = { _, pojo -> pojo.album.albumId },
        isSelected = isSelected,
    ) { _, pojo ->
        val (downloadProgress, downloadIsActive) =
            getDownloadProgress(albumDownloadTasks.find { it.album.albumId == pojo.album.albumId })
        val callbacks = albumCallbacks(pojo)

        Box(modifier = Modifier.aspectRatio(1f)) {
            val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(pojo.album.albumId) {
                imageBitmap.value = pojo.getFullImage(context)
            }

            Thumbnail(
                image = imageBitmap.value,
                borderWidth = null,
                placeholderIcon = Icons.Sharp.Album,
            )

            if (isSelected(pojo)) {
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
                progress = downloadProgress?.toFloat() ?: 0f,
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (downloadIsActive) 3.dp else 5.dp,
                    bottom = 5.dp,
                    start = 5.dp,
                ),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = pojo.album.title,
                    maxLines = if (pojo.album.artist != null && showArtist) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    style = ThouCylinderTheme.typographyExtended.listNormalHeader,
                )
                if (showArtist) {
                    pojo.album.artist?.also { artist ->
                        Text(
                            text = artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ThouCylinderTheme.typographyExtended.listNormalSubtitle,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlbumSmallIcons(pojo = pojo)
                }
            }

            AlbumContextMenuWithButton(
                isLocal = pojo.album.isLocal,
                isInLibrary = pojo.album.isInLibrary,
                isPartiallyDownloaded = pojo.isPartiallyDownloaded,
                callbacks = callbacks,
            )
        }
    }
}
